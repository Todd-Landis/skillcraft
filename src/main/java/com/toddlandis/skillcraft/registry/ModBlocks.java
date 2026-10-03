package com.toddlandis.skillcraft.registry;

import java.util.EnumMap;
import java.util.Map;

import com.toddlandis.skillcraft.SkillCraft;
import com.toddlandis.skillcraft.altar.AttunementAltarBlock;
import com.toddlandis.skillcraft.bench.BenchBlock;
import com.toddlandis.skillcraft.bench.Station;
import com.toddlandis.skillcraft.node.NodeBlock;
import com.toddlandis.skillcraft.node.NodeType;
import com.toddlandis.skillcraft.skill.Skill;
import com.toddlandis.skillcraft.smithing.SmelterBlock;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(SkillCraft.MODID);

    // One node block per NodeType, e.g. "skillcraft:oak_node"
    public static final Map<NodeType, DeferredBlock<NodeBlock>> NODES = new EnumMap<>(NodeType.class);

    static {
        for (NodeType type : NodeType.values()) {
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

    public static final DeferredBlock<AttunementAltarBlock> ATTUNEMENT_ALTAR = BLOCKS.registerBlock("attunement_altar",
            AttunementAltarBlock::new, p -> p
                    .strength(3.5f)
                    .requiresCorrectToolForDrops()
                    .mapColor(MapColor.STONE)
                    .sound(SoundType.TUFF_BRICKS)
                    .lightLevel(state -> 7));
}
