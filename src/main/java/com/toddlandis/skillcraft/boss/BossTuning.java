package com.toddlandis.skillcraft.boss;

import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.toddlandis.skillcraft.combat.DamageStyle;

// Boss balance values, loaded from data/skillcraft/data_maps/entity_type/boss_tuning.json so
// the fight can be tuned without touching code. See docs/BOSS_SLAGHEART.md.
//   health / party_health_scale - base health, and +x per extra party member
//   damage                      - raw damage per attack name (Normal difficulty)
//   windup                      - windup ticks per attack name
//   vent_ticks                  - vulnerability window length per phase (1, 2, 3)
//   vent_multiplier / stoked_multiplier - damage taken while vented / while mites live
//   enrage_seconds              - when the soft enrage starts
//   stances                     - damage taken per damage type in each stance ("banked", "vented")
public record BossTuning(double health, double partyHealthScale, Map<String, Double> damage, Map<String, Integer> windup,
                         Map<String, Integer> ventTicks, double ventMultiplier, double stokedMultiplier, int enrageSeconds,
                         Map<String, Map<DamageStyle, Double>> stances) {
    public static final Codec<BossTuning> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.doubleRange(1, 100000).fieldOf("health").forGetter(BossTuning::health),
            Codec.doubleRange(0, 10).optionalFieldOf("party_health_scale", 0.6).forGetter(BossTuning::partyHealthScale),
            Codec.unboundedMap(Codec.STRING, Codec.DOUBLE).fieldOf("damage").forGetter(BossTuning::damage),
            Codec.unboundedMap(Codec.STRING, Codec.INT).fieldOf("windup").forGetter(BossTuning::windup),
            Codec.unboundedMap(Codec.STRING, Codec.INT).fieldOf("vent_ticks").forGetter(BossTuning::ventTicks),
            Codec.DOUBLE.optionalFieldOf("vent_multiplier", 1.5).forGetter(BossTuning::ventMultiplier),
            Codec.DOUBLE.optionalFieldOf("stoked_multiplier", 0.5).forGetter(BossTuning::stokedMultiplier),
            Codec.INT.optionalFieldOf("enrage_seconds", 360).forGetter(BossTuning::enrageSeconds),
            Codec.unboundedMap(Codec.STRING, Codec.unboundedMap(DamageStyle.CODEC, Codec.DOUBLE)).fieldOf("stances").forGetter(BossTuning::stances)
    ).apply(instance, BossTuning::new));

    public float damage(String attack, float fallback) {
        return damage.getOrDefault(attack, (double) fallback).floatValue();
    }

    public int windup(String attack, int fallback) {
        return windup.getOrDefault(attack, fallback);
    }

    public int ventTicks(int phase) {
        return ventTicks.getOrDefault(String.valueOf(phase), 60);
    }

    public double stanceModifier(String stance, DamageStyle style) {
        Map<DamageStyle, Double> table = stances.get(stance);
        return table == null ? 1.0 : table.getOrDefault(style, 1.0);
    }
}
