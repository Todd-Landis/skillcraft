package com.toddlandis.skillcraft.registry;

import java.util.function.Supplier;

import com.mojang.serialization.Codec;
import com.toddlandis.skillcraft.SkillCraft;

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
}
