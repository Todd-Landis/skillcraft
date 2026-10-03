package com.toddlandis.skillcraft.skill;

import java.util.Locale;

import com.mojang.serialization.Codec;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

// The starter set of skills. Each one gets its own package of mechanics later;
// this enum is the single list everything else (saving, syncing, UI) iterates over.
public enum Skill implements StringRepresentable {
    // Combat
    MELEE(Category.COMBAT),
    RANGED(Category.COMBAT),
    CONSTITUTION(Category.COMBAT),

    // Gathering
    MINING(Category.GATHERING),
    WOODCUTTING(Category.GATHERING),
    FISHING(Category.GATHERING),
    FARMING(Category.GATHERING),

    // Artisan
    SMITHING(Category.ARTISAN),
    CRAFTING(Category.ARTISAN),
    FIREMAKING(Category.ARTISAN),
    CONSTRUCTION(Category.ARTISAN),

    // Support
    AGILITY(Category.SUPPORT),
    SAILING(Category.SUPPORT);

    public enum Category { COMBAT, GATHERING, ARTISAN, SUPPORT }

    // Used in JSON data files, e.g. "woodcutting"
    public static final Codec<Skill> CODEC = StringRepresentable.fromEnum(Skill::values);
    // Used in network packets (sent as the enum index)
    public static final StreamCodec<ByteBuf, Skill> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(i -> values()[i], Skill::ordinal);

    private final Category category;

    Skill(Category category) {
        this.category = category;
    }

    public Category category() {
        return category;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    // Translation key, e.g. "skill.skillcraft.woodcutting" in en_us.json
    public String translationKey() {
        return "skill.skillcraft." + getSerializedName();
    }

    public Component displayName() {
        return Component.translatable(translationKey());
    }

    public static Skill byName(String name) {
        return valueOf(name.toUpperCase(Locale.ROOT));
    }
}
