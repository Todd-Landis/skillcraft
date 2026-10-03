package com.toddlandis.skillcraft.registry;

import com.toddlandis.skillcraft.SkillCraft;
import com.toddlandis.skillcraft.bench.BenchRecipe;
import com.toddlandis.skillcraft.skill.GatheringTool;
import com.toddlandis.skillcraft.skill.SkillRequirements;
import com.toddlandis.skillcraft.skill.SmeltingRecipe;
import com.toddlandis.skillcraft.skill.SkillResource;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

// Data maps attach our JSON data to existing blocks and items. The values live in
// src/main/resources/data/skillcraft/data_maps/, so balancing never needs code changes.
@EventBusSubscriber(modid = SkillCraft.MODID)
public class ModDataMaps {
    // item -> skill levels needed to use it, e.g. iron_pickaxe needs Mining 10
    public static final DataMapType<Item, SkillRequirements> ITEM_REQUIREMENTS = DataMapType
            .builder(Identifier.fromNamespaceAndPath(SkillCraft.MODID, "skill_requirements"), Registries.ITEM, SkillRequirements.CODEC)
            .synced(SkillRequirements.CODEC, false)
            .build();

    // block -> which skill gathers it, the level needed, and the XP it gives
    public static final DataMapType<Block, SkillResource> BLOCK_RESOURCES = DataMapType
            .builder(Identifier.fromNamespaceAndPath(SkillCraft.MODID, "skill_resources"), Registries.BLOCK, SkillResource.CODEC)
            .synced(SkillResource.CODEC, false)
            .build();

    // axe/pickaxe -> gathering tier and power used on skilling nodes
    public static final DataMapType<Item, GatheringTool> GATHERING_TOOLS = DataMapType
            .builder(Identifier.fromNamespaceAndPath(SkillCraft.MODID, "gathering_tools"), Registries.ITEM, GatheringTool.CODEC)
            .synced(GatheringTool.CODEC, false)
            .build();

    // raw ore -> what the Smelter makes from it, coal cost, level, XP and time
    public static final DataMapType<Item, SmeltingRecipe> SMELTING = DataMapType
            .builder(Identifier.fromNamespaceAndPath(SkillCraft.MODID, "smelting"), Registries.ITEM, SmeltingRecipe.CODEC)
            .synced(SmeltingRecipe.CODEC, false)
            .build();

    // item -> how to make it at a Forging Anvil or Crafting Bench
    public static final DataMapType<Item, BenchRecipe> BENCH_RECIPES = DataMapType
            .builder(Identifier.fromNamespaceAndPath(SkillCraft.MODID, "bench_recipes"), Registries.ITEM, BenchRecipe.CODEC)
            .synced(BenchRecipe.CODEC, false)
            .build();

    @SubscribeEvent
    static void register(RegisterDataMapTypesEvent event) {
        event.register(ITEM_REQUIREMENTS);
        event.register(BLOCK_RESOURCES);
        event.register(GATHERING_TOOLS);
        event.register(SMELTING);
        event.register(BENCH_RECIPES);
    }

    public static SmeltingRecipe smelting(ItemStack stack) {
        return stack.isEmpty() ? null : stack.typeHolder().getData(SMELTING);
    }

    public static GatheringTool gatheringTool(ItemStack stack) {
        GatheringTool tool = stack.isEmpty() ? null : stack.typeHolder().getData(GATHERING_TOOLS);
        return tool != null ? tool : GatheringTool.NONE;
    }
}
