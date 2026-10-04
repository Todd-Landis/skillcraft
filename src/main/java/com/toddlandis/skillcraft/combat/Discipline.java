package com.toddlandis.skillcraft.combat;

import java.util.Locale;

import com.mojang.serialization.Codec;
import com.toddlandis.skillcraft.skill.Skill;

import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

// The three ways to fight. A hit's damage type decides its discipline, and the discipline decides
// which skill gets the combat XP and the level damage bonus (Melee, Ranged or Magic). Blessings
// that boost or ward against "melee", "ranged" or "magic" use this too.
public enum Discipline implements StringRepresentable {
    MELEE(Skill.MELEE),
    RANGED(Skill.RANGED),
    MAGIC(Skill.MAGIC);

    public static final Codec<Discipline> CODEC = StringRepresentable.fromEnum(Discipline::values);

    private final Skill skill;

    Discipline(Skill skill) {
        this.skill = skill;
    }

    public Skill skill() {
        return skill;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public Component displayName() {
        return Component.translatable("combat.skillcraft.discipline." + getSerializedName());
    }
}
