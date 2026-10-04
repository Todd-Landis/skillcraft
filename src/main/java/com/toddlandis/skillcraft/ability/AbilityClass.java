package com.toddlandis.skillcraft.ability;

import com.mojang.serialization.Codec;

import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

// Basic abilities are free (many build Momentum), Surges need 50 and spend 25, Ultimates need a
// full bar and spend all of it. An ability's JSON can override these numbers.
public enum AbilityClass implements StringRepresentable {
    BASIC(0, 0),
    SURGE(50, 25),
    ULTIMATE(100, 100);

    public static final Codec<AbilityClass> CODEC = StringRepresentable.fromEnum(AbilityClass::values);

    private final int requires;
    private final int cost;

    AbilityClass(int requires, int cost) {
        this.requires = requires;
        this.cost = cost;
    }

    public int defaultRequires() {
        return requires;
    }

    public int defaultCost() {
        return cost;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    public Component displayName() {
        return Component.translatable("ability_class.skillcraft." + getSerializedName());
    }
}
