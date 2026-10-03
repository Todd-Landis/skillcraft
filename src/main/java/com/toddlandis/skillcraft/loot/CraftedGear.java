package com.toddlandis.skillcraft.loot;

import com.toddlandis.skillcraft.bench.BenchRecipe;
import com.toddlandis.skillcraft.registry.ModDataComponents;
import com.toddlandis.skillcraft.registry.ModDataMaps;
import com.toddlandis.skillcraft.skill.SmeltingRecipe;

import net.minecraft.core.registries.BuiltInRegistries;

import net.minecraft.world.item.ItemStack;

// Gear made at the stations (anything with a bench recipe) should only enter the world through
// players: never generated in chests, never dropped from a mob's starting equipment.
public final class CraftedGear {
    private CraftedGear() {}

    public static boolean isStationGear(ItemStack stack) {
        return !stack.isEmpty() && BenchRecipe.get(stack.getItem()) != null;
    }

    public static boolean isPlayerMade(ItemStack stack) {
        return stack.has(ModDataComponents.CRAFTED_BY.get());
    }

    // Ingots and scrap: anything the Smelter produces
    public static boolean isSmeltedMaterial(ItemStack stack) {
        if (stack.isEmpty()) return false;
        for (SmeltingRecipe recipe : BuiltInRegistries.ITEM.getDataMap(ModDataMaps.SMELTING).values()) {
            if (stack.is(recipe.result())) return true;
        }
        return false;
    }

    // True if this stack must not appear from world generation or mob drops
    public static boolean isUnearned(ItemStack stack) {
        return isStationGear(stack) && !isPlayerMade(stack);
    }
}
