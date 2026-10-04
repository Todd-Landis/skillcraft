package com.toddlandis.skillcraft.ability.behavior;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.toddlandis.skillcraft.ability.AbilityBehavior;
import com.toddlandis.skillcraft.ability.AbilityContext;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

// Battle Trance, Brace, Bulwark, Last Stand, Deadeye, Sprint Burst: a timed effect on yourself.
// Attribute effects scale with the amplifier; the rest read data_maps/mob_effect/effect_tuning.json.
public record Buff(Holder<MobEffect> effect, int duration, int amplifier) implements AbilityBehavior {
    public static final MapCodec<Buff> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BuiltInRegistries.MOB_EFFECT.holderByNameCodec().fieldOf("effect").forGetter(Buff::effect),
            Codec.intRange(1, 12000).fieldOf("duration").forGetter(Buff::duration),
            Codec.intRange(0, 9).optionalFieldOf("amplifier", 0).forGetter(Buff::amplifier)
    ).apply(i, Buff::new));

    @Override
    public MapCodec<Buff> codec() {
        return CODEC;
    }

    @Override
    public boolean execute(AbilityContext context) {
        context.player().addEffect(new MobEffectInstance(effect, duration, amplifier, false, true, true));
        context.level().sendParticles(ParticleTypes.ENCHANT, context.player().getX(), context.player().getY() + 1, context.player().getZ(), 20, 0.4, 0.6, 0.4, 0.4);
        Fx.sound(context.level(), context.player(), SoundEvents.BEACON_POWER_SELECT, 0.6f, 1.4f);
        return true;
    }

    @Override
    public List<Component> details() {
        return List.of(Component.translatable("ability.skillcraft.detail.buff", effect.value().getDisplayName(), String.format("%.1f", duration / 20.0)));
    }
}
