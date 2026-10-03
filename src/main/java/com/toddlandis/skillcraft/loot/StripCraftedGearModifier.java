package com.toddlandis.skillcraft.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

// Global loot modifier: removes station-made gear (tools, weapons, armor, bows...) and smelted
// ingots/scrap from every "chests/..." loot table, so world-generated chests never contain
// crafted items.
// Loaded automatically from data/skillcraft/loot_modifiers/strip_crafted_gear.json
public class StripCraftedGearModifier extends LootModifier {
    public static final MapCodec<StripCraftedGearModifier> CODEC = RecordCodecBuilder.mapCodec(
            instance -> codecStart(instance).apply(instance, StripCraftedGearModifier::new));

    public StripCraftedGearModifier(LootItemCondition[] conditions, int priority) {
        super(conditions, priority);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        if (context.getQueriedLootTableId().getPath().startsWith("chests/")) {
            loot.removeIf(stack -> CraftedGear.isUnearned(stack) || CraftedGear.isSmeltedMaterial(stack));
        }
        return loot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
