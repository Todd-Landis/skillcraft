package com.toddlandis.skillcraft.ability.behavior;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.toddlandis.skillcraft.ability.AbilityBehavior;
import com.toddlandis.skillcraft.ability.AbilityContext;
import com.toddlandis.skillcraft.ability.AbilityRuntime;
import com.toddlandis.skillcraft.registry.ModEffects;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;

// Second Wind: heals `fraction` of max health, spread evenly over `duration` ticks (once a second)
public record HealOverTime(float fraction, int duration) implements AbilityBehavior {
    public static final MapCodec<HealOverTime> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.floatRange(0, 1).fieldOf("fraction").forGetter(HealOverTime::fraction),
            Codec.intRange(20, 1200).fieldOf("duration").forGetter(HealOverTime::duration)
    ).apply(i, HealOverTime::new));

    @Override
    public MapCodec<HealOverTime> codec() {
        return CODEC;
    }

    @Override
    public boolean execute(AbilityContext context) {
        AbilityRuntime runtime = context.runtime();
        int seconds = Math.max(1, duration / 20);
        runtime.healPerSecond = context.player().getMaxHealth() * fraction / seconds;
        runtime.healUntil = context.now() + duration;
        context.player().addEffect(new MobEffectInstance(ModEffects.SECOND_WIND, duration, 0, false, true, true));
        context.level().sendParticles(ParticleTypes.HEART, context.player().getX(), context.player().getY() + 1.8, context.player().getZ(), 4, 0.4, 0.2, 0.4, 0);
        Fx.sound(context.level(), context.player(), SoundEvents.PLAYER_LEVELUP, 0.4f, 1.8f);
        return true;
    }

    @Override
    public List<Component> details() {
        return List.of(Component.translatable("ability.skillcraft.detail.heal", Math.round(fraction * 100), String.format("%.1f", duration / 20.0)));
    }
}
