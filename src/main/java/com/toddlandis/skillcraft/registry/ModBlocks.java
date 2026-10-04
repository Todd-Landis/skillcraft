package com.toddlandis.skillcraft.registry;

import java.util.EnumMap;
import java.util.Map;

import com.toddlandis.skillcraft.SkillCraft;
import com.toddlandis.skillcraft.altar.AttunementAltarBlock;
import com.toddlandis.skillcraft.arena.ExitPortalBlock;
import com.toddlandis.skillcraft.arena.RewardChestBlock;
import com.toddlandis.skillcraft.arena.SummoningBrazierBlock;
import com.toddlandis.skillcraft.bank.BankChestBlock;
import com.toddlandis.skillcraft.bench.BenchBlock;
import com.toddlandis.skillcraft.bench.Station;
import com.toddlandis.skillcraft.node.NodeBlock;
import com.toddlandis.skillcraft.node.NodeType;
import com.toddlandis.skillcraft.skill.Skill;
import com.toddlandis.skillcraft.smithing.SmelterBlock;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(SkillCraft.MODID);

    // One node block per NodeType, e.g. "skillcraft:oak_node"
    public static final Map<NodeType, DeferredBlock<NodeBlock>> NODES = new EnumMap<>(NodeType.class);

    static {
        for (NodeType type : NodeType.values()) {
            if (type.isSpot()) continue; // fishing spots are entities, see fishing/FishingSpotEntity
            boolean wood = type.skill() == Skill.WOODCUTTING;
            NODES.put(type, BLOCKS.registerBlock(type.blockName(), p -> new NodeBlock(type, p), p -> p
                    .strength(-1f, 3600000f) // unbreakable in survival; use the Relocator
                    .noLootTable()
                    .noOcclusion()
                    .mapColor(wood ? MapColor.WOOD : MapColor.STONE)
                    .sound(wood ? SoundType.WOOD : SoundType.STONE)));
        }
    }

    public static final DeferredBlock<SmelterBlock> SMELTER = BLOCKS.registerBlock("smelter",
            SmelterBlock::new, p -> p
                    .strength(3.5f)
                    .requiresCorrectToolForDrops()
                    .mapColor(MapColor.STONE)
                    .sound(SoundType.STONE)
                    .lightLevel(state -> state.getValue(SmelterBlock.LIT) ? 13 : 0));

    public static final DeferredBlock<com.toddlandis.skillcraft.cooking.CookingRangeBlock> COOKING_RANGE = BLOCKS.registerBlock("cooking_range",
            com.toddlandis.skillcraft.cooking.CookingRangeBlock::new, p -> p
                    .strength(3.5f)
                    .requiresCorrectToolForDrops()
                    .mapColor(MapColor.STONE)
                    .sound(SoundType.STONE)
                    .lightLevel(state -> state.getValue(com.toddlandis.skillcraft.cooking.CookingRangeBlock.LIT) ? 13 : 0));

    public static final DeferredBlock<BenchBlock> FORGING_ANVIL = BLOCKS.registerBlock("forging_anvil",
            p -> new BenchBlock(Station.ANVIL, p), p -> p
                    .strength(5.0f, 1200f)
                    .requiresCorrectToolForDrops()
                    .mapColor(MapColor.METAL)
                    .sound(SoundType.ANVIL)
                    .noOcclusion());

    public static final DeferredBlock<BenchBlock> CRAFTING_BENCH = BLOCKS.registerBlock("crafting_bench",
            p -> new BenchBlock(Station.CRAFTING_BENCH, p), p -> p
                    .strength(2.5f)
                    .mapColor(MapColor.WOOD)
                    .sound(SoundType.WOOD));

    public static final DeferredBlock<BankChestBlock> BANK_CHEST = BLOCKS.registerBlock("bank_chest",
            BankChestBlock::new, p -> p
                    .strength(4.0f, 1200f)
                    .requiresCorrectToolForDrops()
                    .mapColor(MapColor.METAL)
                    .sound(SoundType.VAULT)
                    .noOcclusion());

    public static final DeferredBlock<AttunementAltarBlock> ATTUNEMENT_ALTAR = BLOCKS.registerBlock("attunement_altar",
            AttunementAltarBlock::new, p -> p
                    .strength(3.5f)
                    .requiresCorrectToolForDrops()
                    .mapColor(MapColor.STONE)
                    .sound(SoundType.TUFF_BRICKS)
                    .lightLevel(state -> 7));

    // Boss arena entrance (overworld), reward chest and exit (arena only; unbreakable)
    public static final DeferredBlock<SummoningBrazierBlock> SUMMONING_BRAZIER = BLOCKS.registerBlock("summoning_brazier",
            SummoningBrazierBlock::new, p -> p
                    .strength(4.0f, 1200f)
                    .requiresCorrectToolForDrops()
                    .mapColor(MapColor.COLOR_BLACK)
                    .sound(SoundType.NETHER_BRICKS)
                    .noOcclusion()
                    .lightLevel(state -> 13));

    public static final DeferredBlock<RewardChestBlock> REWARD_CHEST = BLOCKS.registerBlock("reward_chest",
            RewardChestBlock::new, p -> p
                    .strength(-1f, 3600000f)
                    .noLootTable()
                    .mapColor(MapColor.GOLD)
                    .sound(SoundType.VAULT)
                    .noOcclusion()
                    .pushReaction(PushReaction.BLOCK)
                    .lightLevel(state -> 10));

    public static final DeferredBlock<ExitPortalBlock> EXIT_PORTAL = BLOCKS.registerBlock("exit_portal",
            ExitPortalBlock::new, p -> p
                    .strength(-1f, 3600000f)
                    .noLootTable()
                    .noCollision()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .sound(SoundType.GLASS)
                    .noOcclusion()
                    .pushReaction(PushReaction.BLOCK)
                    .lightLevel(state -> 11));
}
