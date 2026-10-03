package com.toddlandis.skillcraft.ability.behavior;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.toddlandis.skillcraft.ability.CrowdControl;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

// A debuff an ability puts on what it hits: {"effect": "skillcraft:slowed", "duration": 80, "amplifier": 3}
// Applied through CrowdControl, so bosses, players and Unshackled targets get the right version.
public record EffectSpec(Holder<MobEffect> effect, int duration, int amplifier) {
    public static final Codec<EffectSpec> CODEC = RecordCodecBuilder.create(i -> i.group(
            BuiltInRegistries.MOB_EFFECT.holderByNameCodec().fieldOf("effect").forGetter(EffectSpec::effect),
            Codec.intRange(1, 12000).fieldOf("duration").forGetter(EffectSpec::duration),
            Codec.intRange(0, 9).optionalFieldOf("amplifier", 0).forGetter(EffectSpec::amplifier)
    ).apply(i, EffectSpec::new));

    public void apply(LivingEntity target, Entity source) {
        CrowdControl.apply(target, effect, duration, amplifier, source);
    }

    public Component describe() {
        return Component.translatable("ability.skillcraft.detail.effect", effect.value().getDisplayName(),
                amplifier + 1, String.format("%.1f", duration / 20.0));
    }
}
