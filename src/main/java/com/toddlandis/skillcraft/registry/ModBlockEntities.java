package com.toddlandis.skillcraft.registry;

import java.util.function.Supplier;

import com.toddlandis.skillcraft.SkillCraft;
import com.toddlandis.skillcraft.altar.AttunementAltarBlockEntity;
import com.toddlandis.skillcraft.bench.BenchBlockEntity;
import com.toddlandis.skillcraft.smithing.SmelterBlockEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, SkillCraft.MODID);

    public static final Supplier<BlockEntityType<AttunementAltarBlockEntity>> ATTUNEMENT_ALTAR = BLOCK_ENTITIES.register("attunement_altar",
            () -> new BlockEntityType<>(AttunementAltarBlockEntity::new, ModBlocks.ATTUNEMENT_ALTAR.get()));

    public static final Supplier<BlockEntityType<SmelterBlockEntity>> SMELTER = BLOCK_ENTITIES.register("smelter",
            () -> new BlockEntityType<>(SmelterBlockEntity::new, ModBlocks.SMELTER.get()));

    public static final Supplier<BlockEntityType<com.toddlandis.skillcraft.cooking.CookingRangeBlockEntity>> COOKING_RANGE = BLOCK_ENTITIES.register("cooking_range",
            () -> new BlockEntityType<>(com.toddlandis.skillcraft.cooking.CookingRangeBlockEntity::new, ModBlocks.COOKING_RANGE.get()));

    // Shared by the Forging Anvil and Crafting Bench
    public static final Supplier<BlockEntityType<BenchBlockEntity>> BENCH = BLOCK_ENTITIES.register("bench",
            () -> new BlockEntityType<>(BenchBlockEntity::new, ModBlocks.FORGING_ANVIL.get(), ModBlocks.CRAFTING_BENCH.get()));
}
