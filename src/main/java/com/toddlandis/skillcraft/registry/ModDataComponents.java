package com.toddlandis.skillcraft.registry;

import java.util.function.Supplier;

import com.mojang.serialization.Codec;
import com.toddlandis.skillcraft.SkillCraft;
import com.toddlandis.skillcraft.loot.EarnedBy;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredRegister;

// Extra data stored on item stacks
public class ModDataComponents {
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, SkillCraft.MODID);

    // Name of the player who made this gear at a Forging Anvil or Crafting Bench. Gear without it
    // is never found in world chests or dropped by mobs (see loot/CraftedGear).
    public static final Supplier<DataComponentType<String>> CRAFTED_BY = COMPONENTS.registerComponentType("crafted_by",
            builder -> builder.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8));

    // Which ability a Codex teaches (see ability/CodexItem)
    public static final Supplier<DataComponentType<net.minecraft.resources.Identifier>> CODEX_ABILITY = COMPONENTS.registerComponentType("codex_ability",
            builder -> builder.persistent(net.minecraft.resources.Identifier.CODEC).networkSynchronized(net.minecraft.resources.Identifier.STREAM_CODEC));

    // Which node an Elemental Heart attunes into (see node/ElementalHeartItem)
    public static final Supplier<DataComponentType<com.toddlandis.skillcraft.node.NodeType>> HEART_NODE = COMPONENTS.registerComponentType("heart_node",
            builder -> builder.persistent(com.toddlandis.skillcraft.node.NodeType.CODEC)
                    .networkSynchronized(ByteBufCodecs.idMapper(i -> com.toddlandis.skillcraft.node.NodeType.values()[i], Enum::ordinal)));

    // Which element a Ley Tuner is tuned to (see leycraft/LeyTunerItem)
    public static final Supplier<DataComponentType<com.toddlandis.skillcraft.leycraft.LeyElement>> LEY_TUNING = COMPONENTS.registerComponentType("ley_tuning",
            builder -> builder.persistent(com.toddlandis.skillcraft.leycraft.LeyElement.CODEC)
                    .networkSynchronized(ByteBufCodecs.idMapper(i -> com.toddlandis.skillcraft.leycraft.LeyElement.values()[i], Enum::ordinal)));

    // Boss and player for gear won from a boss, e.g. the Emberfall Maul (see loot/EarnedBy)
    public static final Supplier<DataComponentType<EarnedBy>> EARNED_BY = COMPONENTS.registerComponentType("earned_by",
            builder -> builder.persistent(EarnedBy.CODEC).networkSynchronized(EarnedBy.STREAM_CODEC));
}
