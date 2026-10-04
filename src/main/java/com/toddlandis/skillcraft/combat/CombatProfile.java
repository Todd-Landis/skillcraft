package com.toddlandis.skillcraft.combat;

import java.util.Map;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

// Data map value for a monster type: how much damage it takes from each damage type
// (multipliers, 1.0 when missing) and which type its own melee hits deal.
// data/skillcraft/data_maps/entity_type/combat_profiles.json
public record CombatProfile(Map<DamageStyle, Double> modifiers, Optional<DamageStyle> attack) {
    public static final CombatProfile NEUTRAL = new CombatProfile(Map.of(), Optional.empty());

    public static final Codec<CombatProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(DamageStyle.CODEC, Codec.doubleRange(0, 10)).optionalFieldOf("modifiers", Map.of()).forGetter(CombatProfile::modifiers),
            DamageStyle.CODEC.optionalFieldOf("attack").forGetter(CombatProfile::attack)
    ).apply(instance, CombatProfile::new));

    public double modifier(DamageStyle style) {
        return modifiers.getOrDefault(style, 1.0);
    }

    public DamageStyle attackStyle() {
        return attack.orElse(DamageStyle.CRUSH);
    }
}
