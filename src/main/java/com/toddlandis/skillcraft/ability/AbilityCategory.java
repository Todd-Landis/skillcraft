package com.toddlandis.skillcraft.ability;

import com.mojang.serialization.Codec;

import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

// The Ability Book's tabs. Spells and Prayers are reserved for the Magic and Prayer skills: the
// book shows them locked, and the framework already supports their item costs and toggles.
public enum AbilityCategory implements StringRepresentable {
    MELEE(0xFFD04040, false),
    RANGED(0xFF50B848, false),
    DEFENCE(0xFF4A86E0, false),
    WEAPON_ART(0xFFE0A030, false),
    UTILITY(0xFFA060D0, false),
    SPELL(0xFF40C0D0, true),
    PRAYER(0xFFE8E0A0, true);

    public static final Codec<AbilityCategory> CODEC = StringRepresentable.fromEnum(AbilityCategory::values);

    private final int color;
    private final boolean comingLater;

    AbilityCategory(int color, boolean comingLater) {
        this.color = color;
        this.comingLater = comingLater;
    }

    // Accent colour for icons, card borders and tabs
    public int color() {
        return color;
    }

    // Shown as a locked tab until its skill exists
    public boolean comingLater() {
        return comingLater;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    public Component displayName() {
        return Component.translatable("ability_category.skillcraft." + getSerializedName());
    }
}
