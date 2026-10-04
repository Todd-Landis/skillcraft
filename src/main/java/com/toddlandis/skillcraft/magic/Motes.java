package com.toddlandis.skillcraft.magic;

import com.toddlandis.skillcraft.combat.DamageStyle;
import com.toddlandis.skillcraft.registry.ModItems;

import net.minecraft.world.item.Item;

// Motes are the spell reagents: one per element, crafted at the Crafting Bench. A bolt costs one
// mote of its element; a staff of that element supplies them for free (MagicFocus.supplies).
public final class Motes {
    private Motes() {}

    public static Item of(DamageStyle element) {
        return switch (element) {
            case FIRE -> ModItems.EMBER_MOTE.get();
            case FROST -> ModItems.FROST_MOTE.get();
            case STORM -> ModItems.STORM_MOTE.get();
            default -> ModItems.ARCANE_MOTE.get();
        };
    }
}
