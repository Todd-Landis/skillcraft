package com.toddlandis.skillcraft.combat;

import java.util.Map;

import com.mojang.serialization.Codec;

// Data map value: typed defence of one armour piece, in percentage points per damage group,
// e.g. {"stab": 1.2, "slash": 1.6, "crush": -0.8, "ranged": 1.2}. Worn pieces add up; the total
// reduces (or, if negative, increases) damage of that group. See docs/COMBAT.md.
public record ArmorDefence(Map<DamageStyle.Group, Double> values) {
    public static final Codec<ArmorDefence> CODEC = Codec.unboundedMap(DamageStyle.Group.CODEC, Codec.doubleRange(-100, 100))
            .xmap(ArmorDefence::new, ArmorDefence::values);

    public double get(DamageStyle.Group group) {
        return values.getOrDefault(group, 0.0);
    }
}
