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

// Riposte: for `window` ticks, an entity's melee or projectile hit is negated; then your next
// melee hit within `counter_window` ticks deals +counter_bonus and gives `momentum`
// (resolved in AbilityEvents)
public record Parry(int window, int counterWindow, float counterBonus, int momentum) implements AbilityBehavior {
    public static final MapCodec<Parry> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.intRange(1, 100).fieldOf("window").forGetter(Parry::window),
            Codec.intRange(1, 200).fieldOf("counter_window").forGetter(Parry::counterWindow),
            Codec.floatRange(0, 10).fieldOf("counter_bonus").forGetter(Parry::counterBonus),
            Codec.intRange(0, 100).optionalFieldOf("momentum", 0).forGetter(Parry::momentum)
    ).apply(i, Parry::new));

    @Override
    public MapCodec<Parry> codec() {
        return CODEC;
    }

    @Override
    public boolean execute(AbilityContext context) {
        AbilityRuntime runtime = context.runtime();
        runtime.parryUntil = context.now() + window;
        runtime.counterBonus = counterBonus;
        runtime.counterMomentum = momentum;
        runtime.counterWindow = counterWindow;
        context.player().addEffect(new MobEffectInstance(ModEffects.PARRYING, window, 0, false, true, true));
        context.level().sendParticles(ParticleTypes.ENCHANTED_HIT, context.player().getX(), context.player().getY() + 1.2, context.player().getZ(), 12, 0.5, 0.5, 0.5, 0.1);
        Fx.sound(context.level(), context.player(), SoundEvents.ARMOR_EQUIP_IRON.value(), 1f, 1.6f);
        return true;
    }

    @Override
    public List<Component> details() {
        return List.of(Component.translatable("ability.skillcraft.detail.parry", String.format("%.1f", window / 20.0),
                Math.round(counterBonus * 100), String.format("%.1f", counterWindow / 20.0), momentum));
    }
}
