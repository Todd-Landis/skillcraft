package com.toddlandis.skillcraft.combat;

import java.util.Locale;

import com.mojang.serialization.Codec;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

// The combat triangle's damage types. Monsters take more or less from each (combat_profiles),
// armour defends against each group (armor_defences). Each type also belongs to a discipline
// (melee, ranged or magic), which decides the skill that gets the XP and the level bonus.
// The four magic types are the elements cast from wands and staves (docs/AGILITY_MAGIC_PRAYER.md).
public enum DamageStyle implements StringRepresentable {
    STAB(Group.STAB, ChatFormatting.AQUA),
    SLASH(Group.SLASH, ChatFormatting.RED),
    CRUSH(Group.CRUSH, ChatFormatting.GOLD),
    LIGHT(Group.RANGED, ChatFormatting.GREEN),
    MEDIUM(Group.RANGED, ChatFormatting.DARK_GREEN),
    HEAVY(Group.RANGED, ChatFormatting.DARK_AQUA),
    FIRE(Group.MAGIC, ChatFormatting.YELLOW),
    FROST(Group.MAGIC, ChatFormatting.BLUE),
    STORM(Group.MAGIC, ChatFormatting.WHITE),
    ARCANE(Group.MAGIC, ChatFormatting.LIGHT_PURPLE);

    // What armour defends against: the three melee types, "ranged" (all ammo weights) and "magic" (all elements)
    public enum Group implements StringRepresentable {
        STAB, SLASH, CRUSH, RANGED, MAGIC;

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

    public boolean isMagic() {
        return group == Group.MAGIC;
    }

    public Discipline discipline() {
        return switch (group) {
            case RANGED -> Discipline.RANGED;
            case MAGIC -> Discipline.MAGIC;
            default -> Discipline.MELEE;
        };
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public Component displayName() {
        return Component.translatable("combat.skillcraft.style." + getSerializedName()).withStyle(color);
    }
}
