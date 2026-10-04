package com.toddlandis.skillcraft.ability.behavior;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.toddlandis.skillcraft.ability.AbilityBehavior;
import com.toddlandis.skillcraft.ability.AbilityContext;
import com.toddlandis.skillcraft.ability.AbilityDamage;
import com.toddlandis.skillcraft.ability.CrowdControl;
import com.toddlandis.skillcraft.ability.DelayedStrikes;
import com.toddlandis.skillcraft.combat.DamageStyle;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

// Starfall: mark the block you're aiming at (up to `range` away); after `delay` ticks everything
// within `radius` of it takes `damage` x the focus's power, with optional knockback and fire.
// Needs a block to aim at, so it never fires into the sky. The spot shows a warning ring meanwhile.
public record GroundTarget(float range, float radius, int delay, float damage, Optional<DamageStyle> style, float knockback, int ignite)
        implements AbilityBehavior {
    public static final MapCodec<GroundTarget> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.floatRange(1, 48).fieldOf("range").forGetter(GroundTarget::range),
            Codec.floatRange(0.5f, 10).fieldOf("radius").forGetter(GroundTarget::radius),
            Codec.intRange(0, 100).optionalFieldOf("delay", 20).forGetter(GroundTarget::delay),
            Codec.floatRange(0, 10).fieldOf("damage").forGetter(GroundTarget::damage),
            DamageStyle.CODEC.optionalFieldOf("style").forGetter(GroundTarget::style),
            Codec.floatRange(0, 5).optionalFieldOf("knockback", 0f).forGetter(GroundTarget::knockback),
            Codec.intRange(0, 400).optionalFieldOf("ignite", 0).forGetter(GroundTarget::ignite)
    ).apply(i, GroundTarget::new));

    @Override
    public MapCodec<GroundTarget> codec() {
        return CODEC;
    }

    @Override
    public boolean execute(AbilityContext context) {
        ServerPlayer player = context.player();
        Vec3 eye = player.getEyePosition();
        BlockHitResult hit = context.level().clip(new ClipContext(eye, eye.add(player.getViewVector(1f).scale(range)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (hit.getType() == HitResult.Type.MISS) {
            context.runtime().failKey = "message.skillcraft.ability_no_spot";
            return false;
        }
        Vec3 spot = hit.getLocation();
        DamageStyle element = style.orElse(AbilityDamage.defaultStyle(player));
        float amount = (float) (AbilityDamage.basePower(player, element) * damage);
        ParticleOptions particle = Fx.element(element) != null ? Fx.element(element) : ParticleTypes.END_ROD;
        context.level().playSound(null, spot.x, spot.y, spot.z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1f, 1.8f);
        DelayedStrikes.schedule(context.level(), delay,
                level -> Fx.ring(level, spot, radius, ParticleTypes.END_ROD),
                level -> strike(level, player, spot, element, amount, particle));
        return true;
    }

    private void strike(ServerLevel level, ServerPlayer player, Vec3 spot, DamageStyle element, float amount, ParticleOptions particle) {
        for (int y = 12; y > 0; y--) level.sendParticles(ParticleTypes.END_ROD, spot.x, spot.y + y, spot.z, 2, 0.1, 0.2, 0.1, 0.01);
        for (float r = 1; r <= radius; r += 0.75f) Fx.ring(level, spot, r, particle);
        level.sendParticles(ParticleTypes.EXPLOSION, spot.x, spot.y + 0.5, spot.z, 1, 0, 0, 0, 0);
        level.playSound(null, spot.x, spot.y, spot.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1f, 1.4f);
        if (!player.isAlive() || player.level() != level) return;
        AABB box = new AABB(spot, spot).inflate(radius, 3, radius);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player && e.isAlive() && CrowdControl.canAffect(player, e)
                && e.position().subtract(spot).horizontalDistance() <= radius + e.getBbWidth() / 2)) {
            if (!AbilityDamage.hurt(player, target, amount, element)) continue;
            if (ignite > 0 && !target.fireImmune()) target.igniteForTicks(ignite);
            if (knockback > 0) CrowdControl.knockback(target, spot, player.getLookAngle(), knockback);
        }
    }

    @Override
    public List<Component> details() {
        List<Component> lines = new ArrayList<>();
        lines.add(Fx.damage(damage, style));
        lines.add(Fx.blocks("radius", radius));
        lines.add(Component.translatable("ability.skillcraft.detail.ground_target", String.format("%.0f", range), String.format("%.1f", delay / 20.0)));
        if (ignite > 0) lines.add(Component.translatable("ability.skillcraft.detail.ignite", String.format("%.1f", ignite / 20.0)));
        if (knockback > 0) lines.add(Component.translatable("ability.skillcraft.detail.knockback"));
        return lines;
    }
}
