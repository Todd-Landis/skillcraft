package com.toddlandis.skillcraft.loot;

import com.toddlandis.skillcraft.SkillCraft;
import com.toddlandis.skillcraft.bench.BenchRecipe;
import com.toddlandis.skillcraft.registry.ModDataComponents;
import com.toddlandis.skillcraft.registry.ModDataMaps;
import com.toddlandis.skillcraft.skill.SmeltingRecipe;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import net.minecraft.world.item.ItemStack;

// Gear made at the stations (anything with a bench recipe) should only enter the world through
// players: never generated in chests, never dropped from a mob's starting equipment.
public final class CraftedGear {
    private CraftedGear() {}

    // Bench-made consumables (ammo, boss keys) aren't gear; see data/skillcraft/tags/item/consumables.json
    public static final TagKey<Item> CONSUMABLES = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(SkillCraft.MODID, "consumables"));

    public static boolean isStationGear(ItemStack stack) {
        return !stack.isEmpty() && BenchRecipe.get(stack.getItem()) != null && !stack.is(CONSUMABLES);
    }

    // Made at a station by a player, or won from a boss by a player
    public static boolean isPlayerMade(ItemStack stack) {
        return stack.has(ModDataComponents.CRAFTED_BY.get()) || stack.has(ModDataComponents.EARNED_BY.get());
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
