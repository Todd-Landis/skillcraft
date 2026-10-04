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

// Sweeping Arc: hits every enemy in a frontal arc. Needs at least one enemy in it.
public record Cleave(float radius, float arc, float damage, Optional<DamageStyle> style) implements AbilityBehavior {
    public static final MapCodec<Cleave> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.floatRange(0, 12).fieldOf("radius").forGetter(Cleave::radius),
            Codec.floatRange(10, 360).fieldOf("arc").forGetter(Cleave::arc),
            Codec.floatRange(0, 10).fieldOf("damage").forGetter(Cleave::damage),
            DamageStyle.CODEC.optionalFieldOf("style").forGetter(Cleave::style)
    ).apply(i, Cleave::new));

    @Override
    public MapCodec<Cleave> codec() {
        return CODEC;
    }

    @Override
    public boolean execute(AbilityContext context) {
        ServerPlayer player = context.player();
        List<LivingEntity> targets = AbilityTargeting.inArc(player, radius, arc);
        if (targets.isEmpty()) return false;
        player.swing(InteractionHand.MAIN_HAND, true);
        for (LivingEntity target : targets) AbilityDamage.strike(player, target, damage, style.orElse(null));
        Vec3 look = player.getLookAngle().multiply(1, 0, 1).normalize();
        Vec3 front = player.position().add(look.scale(radius * 0.6)).add(0, 1, 0);
        context.level().sendParticles(ParticleTypes.SWEEP_ATTACK, front.x, front.y, front.z, 3, radius * 0.3, 0.1, radius * 0.3, 0);
        Fx.sound(context.level(), player, SoundEvents.PLAYER_ATTACK_SWEEP, 1f, 0.9f);
        return true;
    }

    @Override
    public List<Component> details() {
        return List.of(Fx.damage(damage, style), Component.translatable("ability.skillcraft.detail.arc", Math.round(arc), String.format("%.1f", radius)));
    }
}
