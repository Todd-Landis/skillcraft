package com.toddlandis.skillcraft.ability.behavior;

import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.toddlandis.skillcraft.ability.AbilityBehavior;
import com.toddlandis.skillcraft.ability.AbilityContext;
import com.toddlandis.skillcraft.ability.AbilityDamage;
import com.toddlandis.skillcraft.ability.AbilityTargeting;
import com.toddlandis.skillcraft.combat.DamageStyle;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

// Lunge: dash up to `distance` blocks forward (stopping at walls and short of the first enemy in
// the way) and strike that enemy. Dashes even with nothing to hit.
public record DashStrike(float distance, float damage, Optional<DamageStyle> style) implements AbilityBehavior {
    public static final MapCodec<DashStrike> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.floatRange(0, 12).fieldOf("distance").forGetter(DashStrike::distance),
            Codec.floatRange(0, 10).fieldOf("damage").forGetter(DashStrike::damage),
            DamageStyle.CODEC.optionalFieldOf("style").forGetter(DashStrike::style)
    ).apply(i, DashStrike::new));

    @Override
    public MapCodec<DashStrike> codec() {
        return CODEC;
    }

    @Override
    public boolean execute(AbilityContext context) {
        ServerPlayer player = context.player();
        Vec3 dir = player.getLookAngle().multiply(1, 0, 1).normalize();
        if (dir.lengthSqr() < 1.0E-4) return false;
        double reach = player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ENTITY_INTERACTION_RANGE);
        LivingEntity target = AbilityTargeting.lookTarget(player, distance + reach);
        double travel = AbilityTargeting.clearDistance(player, dir, distance);
        if (target != null) travel = Math.min(travel, Math.max(0, player.position().subtract(target.position()).horizontalDistance() - target.getBbWidth() / 2 - 1.0));
        Vec3 from = player.position();
        Vec3 to = from.add(dir.scale(travel));
        for (int s = 0; s <= 6; s++) {
            Vec3 p = from.lerp(to, s / 6.0);
            context.level().sendParticles(ParticleTypes.CLOUD, p.x, p.y + 0.3, p.z, 1, 0.1, 0.1, 0.1, 0.01);
        }
        player.teleportTo(context.level(), to.x, to.y, to.z, java.util.Set.of(), player.getYRot(), player.getXRot(), true);
        player.resetFallDistance();
        Fx.sound(context.level(), player, SoundEvents.PLAYER_ATTACK_SWEEP, 0.8f, 1.3f);
        if (target != null && player.distanceTo(target) <= reach + target.getBbWidth() && AbilityTargeting.canSee(player, target)) {
            player.swing(InteractionHand.MAIN_HAND, true);
            AbilityDamage.strike(player, target, damage, style.orElse(null));
            Fx.at(context.level(), target, ParticleTypes.CRIT, 10, 0.4);
        }
        return true;
    }

    @Override
    public List<Component> details() {
        return List.of(Fx.damage(damage, style), Fx.blocks("dash", distance));
    }
}
