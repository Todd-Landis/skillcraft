package com.toddlandis.skillcraft.registry;

import com.toddlandis.skillcraft.SkillCraft;
import com.toddlandis.skillcraft.bench.BenchRecipe;
import com.toddlandis.skillcraft.boss.BossTuning;
import com.toddlandis.skillcraft.combat.ArmorDefence;
import com.toddlandis.skillcraft.combat.CombatProfile;
import com.toddlandis.skillcraft.combat.WeaponStyle;
import com.toddlandis.skillcraft.skill.GatheringTool;
import com.toddlandis.skillcraft.skill.SkillRequirements;
import com.toddlandis.skillcraft.skill.SmeltingRecipe;
import com.toddlandis.skillcraft.skill.SkillResource;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
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

    // weapon -> its melee damage type (stab/slash/crush); see docs/COMBAT.md
    public static final DataMapType<Item, WeaponStyle> WEAPON_STYLES = DataMapType
            .builder(Identifier.fromNamespaceAndPath(SkillCraft.MODID, "weapon_styles"), Registries.ITEM, WeaponStyle.CODEC)
            .synced(WeaponStyle.CODEC, false)
            .build();

    // armour piece -> typed defence against stab/slash/crush/ranged
    public static final DataMapType<Item, ArmorDefence> ARMOR_DEFENCES = DataMapType
            .builder(Identifier.fromNamespaceAndPath(SkillCraft.MODID, "armor_defences"), Registries.ITEM, ArmorDefence.CODEC)
            .synced(ArmorDefence.CODEC, false)
            .build();

    // monster -> damage taken per damage type, and the type its melee deals
    public static final DataMapType<EntityType<?>, CombatProfile> COMBAT_PROFILES = DataMapType
            .builder(Identifier.fromNamespaceAndPath(SkillCraft.MODID, "combat_profiles"), Registries.ENTITY_TYPE, CombatProfile.CODEC)
            .synced(CombatProfile.CODEC, false)
            .build();

    // boss -> health, damage, timings (server-side only)
    public static final DataMapType<EntityType<?>, BossTuning> BOSS_TUNING = DataMapType
            .builder(Identifier.fromNamespaceAndPath(SkillCraft.MODID, "boss_tuning"), Registries.ENTITY_TYPE, BossTuning.CODEC)
            .build();

    // status effect -> numbers for ability buffs and debuffs (see ability/EffectTuning)
    public static final DataMapType<net.minecraft.world.effect.MobEffect, com.toddlandis.skillcraft.ability.EffectTuning> EFFECT_TUNING = DataMapType
            .builder(Identifier.fromNamespaceAndPath(SkillCraft.MODID, "effect_tuning"), Registries.MOB_EFFECT, com.toddlandis.skillcraft.ability.EffectTuning.CODEC)
            .build();

    // wand/staff -> kind, bolt power, element and cast cooldown (docs/AGILITY_MAGIC_PRAYER.md)
    public static final DataMapType<Item, com.toddlandis.skillcraft.magic.MagicFocus> MAGIC_FOCI = DataMapType
            .builder(Identifier.fromNamespaceAndPath(SkillCraft.MODID, "magic_focus"), Registries.ITEM, com.toddlandis.skillcraft.magic.MagicFocus.CODEC)
            .synced(com.toddlandis.skillcraft.magic.MagicFocus.CODEC, false)
            .build();

    // bone, skull... -> Prayer XP for burying it (offering it at a Votive Shrine multiplies it)
    public static final DataMapType<Item, com.toddlandis.skillcraft.prayer.PrayerOffering> PRAYER_OFFERINGS = DataMapType
            .builder(Identifier.fromNamespaceAndPath(SkillCraft.MODID, "prayer_offerings"), Registries.ITEM, com.toddlandis.skillcraft.prayer.PrayerOffering.CODEC)
            .synced(com.toddlandis.skillcraft.prayer.PrayerOffering.CODEC, false)
            .build();

    @SubscribeEvent
    static void register(RegisterDataMapTypesEvent event) {
        event.register(MAGIC_FOCI);
        event.register(PRAYER_OFFERINGS);
        event.register(EFFECT_TUNING);
        event.register(WEAPON_STYLES);
        event.register(ARMOR_DEFENCES);
        event.register(COMBAT_PROFILES);
        event.register(BOSS_TUNING);
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
