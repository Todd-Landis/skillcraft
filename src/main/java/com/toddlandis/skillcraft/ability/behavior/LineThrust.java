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

// Skewer: a straight thrust that hits every enemy along a line (stopping at walls)
public record LineThrust(float length, float width, float damage, Optional<DamageStyle> style) implements AbilityBehavior {
    public static final MapCodec<LineThrust> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.floatRange(0, 16).fieldOf("length").forGetter(LineThrust::length),
            Codec.floatRange(0, 3).optionalFieldOf("width", 0.6f).forGetter(LineThrust::width),
            Codec.floatRange(0, 10).fieldOf("damage").forGetter(LineThrust::damage),
            DamageStyle.CODEC.optionalFieldOf("style").forGetter(LineThrust::style)
    ).apply(i, LineThrust::new));

    @Override
    public MapCodec<LineThrust> codec() {
        return CODEC;
    }

    @Override
    public boolean execute(AbilityContext context) {
        ServerPlayer player = context.player();
        List<LivingEntity> targets = AbilityTargeting.inLine(player, length, width);
        if (targets.isEmpty()) return false;
        player.swing(InteractionHand.MAIN_HAND, true);
        for (LivingEntity target : targets) AbilityDamage.strike(player, target, damage, style.orElse(null));
        Vec3 eye = player.getEyePosition();
        Vec3 dir = player.getViewVector(1f);
        for (float d = 0.5f; d <= length; d += 0.5f) {
            Vec3 p = eye.add(dir.scale(d));
            context.level().sendParticles(ParticleTypes.CRIT, p.x, p.y - 0.2, p.z, 1, 0, 0, 0, 0);
        }
        Fx.sound(context.level(), player, SoundEvents.TRIDENT_THROW.value(), 1f, 1.2f);
        return true;
    }

    @Override
    public List<Component> details() {
        return List.of(Fx.damage(damage, style), Fx.blocks("line", length));
    }
}
