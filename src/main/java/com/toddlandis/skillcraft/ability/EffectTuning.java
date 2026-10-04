package com.toddlandis.skillcraft.ability;

import java.util.Map;

import com.mojang.serialization.Codec;

import com.toddlandis.skillcraft.registry.ModDataMaps;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;

// Numbers for the marker effects (data_maps/mob_effect/effect_tuning.json), e.g.
//   "skillcraft:battle_trance": {"damage_dealt": 0.25, "damage_taken": 0.10}
// Read with a fallback so a missing entry never breaks combat.
public record EffectTuning(Map<String, Double> values) {
    public static final Codec<EffectTuning> CODEC = Codec.unboundedMap(Codec.STRING, Codec.DOUBLE).xmap(EffectTuning::new, EffectTuning::values);

    public static double get(Holder<MobEffect> effect, String key, double fallback) {
        EffectTuning tuning = effect.getData(ModDataMaps.EFFECT_TUNING);
        if (tuning == null) return fallback;
        Double value = tuning.values().get(key);
        return value != null ? value : fallback;
    }
}
