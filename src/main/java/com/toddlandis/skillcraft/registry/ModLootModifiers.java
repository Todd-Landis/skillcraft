package com.toddlandis.skillcraft.registry;

import java.util.function.Supplier;

import com.mojang.serialization.MapCodec;
import com.toddlandis.skillcraft.SkillCraft;
import com.toddlandis.skillcraft.loot.StripCraftedGearModifier;

import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class ModLootModifiers {
    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, SkillCraft.MODID);

    public static final Supplier<MapCodec<StripCraftedGearModifier>> STRIP_CRAFTED_GEAR =
            LOOT_MODIFIERS.register("strip_crafted_gear", () -> StripCraftedGearModifier.CODEC);
}
