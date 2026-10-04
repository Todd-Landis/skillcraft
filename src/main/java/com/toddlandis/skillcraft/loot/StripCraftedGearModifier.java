package com.toddlandis.skillcraft.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import com.toddlandis.skillcraft.node.NodeType;
import com.toddlandis.skillcraft.skill.Skill;
import com.toddlandis.skillcraft.skill.SkillManager;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

// Global loot modifier: removes station-made gear (tools, weapons, armor, bows...) and smelted
// ingots/scrap from every "chests/..." loot table, so world-generated chests never contain
// crafted items. Also filters vanilla fishing catches (see doApply).
// Loaded automatically from data/skillcraft/loot_modifiers/strip_crafted_gear.json
public class StripCraftedGearModifier extends LootModifier {
    public static final MapCodec<StripCraftedGearModifier> CODEC = RecordCodecBuilder.mapCodec(
            instance -> codecStart(instance).apply(instance, StripCraftedGearModifier::new));

    public StripCraftedGearModifier(LootItemCondition[] conditions, int priority) {
        super(conditions, priority);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        String path = context.getQueriedLootTableId().getPath();
        if (path.startsWith("chests/")) {
            loot.removeIf(stack -> CraftedGear.isUnearned(stack) || CraftedGear.isSmeltedMaterial(stack));
        } else if (path.startsWith("gameplay/fishing")) {
            // Vanilla fishing: its treasure (bows, rods) is gear too, and fish above the angler's
            // Fishing level slip off the hook. It gives no Fishing XP; spots are for training.
            loot.removeIf(CraftedGear::isUnearned);
            if (context.getOptionalParameter(LootContextParams.ATTACKING_ENTITY) instanceof Player angler) {
                int fishing = SkillManager.level(angler, Skill.FISHING);
                loot.removeIf(stack -> {
                    NodeType spot = NodeType.byInput(stack.getItem());
                    return spot != null && spot.isSpot() && fishing < spot.level();
                });
            }
        }
        return loot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
