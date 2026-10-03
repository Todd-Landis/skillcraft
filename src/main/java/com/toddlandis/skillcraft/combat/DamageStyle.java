package com.toddlandis.skillcraft.combat;

import java.util.Locale;

import com.mojang.serialization.Codec;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

// The combat triangle's damage types. Monsters take more or less from each (combat_profiles),
// armour defends against each group (armor_defences). Magic is reserved for a later phase.
public enum DamageStyle implements StringRepresentable {
    STAB(Group.STAB, ChatFormatting.AQUA),
    SLASH(Group.SLASH, ChatFormatting.RED),
    CRUSH(Group.CRUSH, ChatFormatting.GOLD),
    LIGHT(Group.RANGED, ChatFormatting.GREEN),
    MEDIUM(Group.RANGED, ChatFormatting.DARK_GREEN),
    HEAVY(Group.RANGED, ChatFormatting.DARK_AQUA);

    // What armour defends against: the three melee types and "ranged" (all ammo weights)
    public enum Group implements StringRepresentable {
        STAB, SLASH, CRUSH, RANGED;

        public static final Codec<Group> CODEC = StringRepresentable.fromEnum(Group::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

        public Component displayName() {
            return Component.translatable("combat.skillcraft.group." + getSerializedName());
        }
    }

    public static final Codec<DamageStyle> CODEC = StringRepresentable.fromEnum(DamageStyle::values);

    private final Group group;
    private final ChatFormatting color;

    DamageStyle(Group group, ChatFormatting color) {
        this.group = group;
        this.color = color;
    }

    public Group group() {
        return group;
    }

    public boolean isRanged() {
        return group == Group.RANGED;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public Component displayName() {
        return Component.translatable("combat.skillcraft.style." + getSerializedName()).withStyle(color);
    }
}
