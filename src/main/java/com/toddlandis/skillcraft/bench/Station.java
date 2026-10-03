package com.toddlandis.skillcraft.bench;

import java.util.Locale;

import com.mojang.serialization.Codec;
import com.toddlandis.skillcraft.skill.Skill;

import net.minecraft.util.StringRepresentable;

// Recipe benches: stations that list recipes, take ingredients when you queue, and work
// through the queue over time.
public enum Station implements StringRepresentable {
    ANVIL(Skill.SMITHING),
    CRAFTING_BENCH(Skill.CRAFTING);

    public static final Codec<Station> CODEC = StringRepresentable.fromEnum(Station::values);

    private final Skill skill;

    Station(Skill skill) {
        this.skill = skill;
    }

    public Skill skill() {
        return skill;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
