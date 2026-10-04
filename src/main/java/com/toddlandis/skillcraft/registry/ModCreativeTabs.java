package com.toddlandis.skillcraft.registry;

import com.toddlandis.skillcraft.SkillCraft;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SkillCraft.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.skillcraft"))
            .icon(() -> ModItems.ATTUNEMENT_ALTAR.get().getDefaultInstance())
            .displayItems((params, output) -> {
                output.accept(ModItems.ATTUNEMENT_ALTAR.get());
                output.accept(ModItems.RELOCATOR.get());
                for (com.toddlandis.skillcraft.node.NodeType type : com.toddlandis.skillcraft.node.NodeType.values()) {
                    output.accept(com.toddlandis.skillcraft.node.ElementalHeartItem.of(type));
                }
                output.accept(ModItems.SMELTER.get());
                output.accept(ModItems.COOKING_RANGE.get());
                output.accept(ModItems.SEARED_REEF_FISH.get());
                output.accept(ModItems.PREPARED_PUFFERFISH.get());
                output.accept(ModItems.BURNT_FOOD.get());
                output.accept(ModItems.FORGING_ANVIL.get());
                output.accept(ModItems.CRAFTING_BENCH.get());
                output.accept(ModItems.BANK_CHEST.get());
                output.accept(ModItems.SUMMONING_BRAZIER.get());
                output.accept(ModItems.EMBER_SIGIL.get());
                ModItems.WARHAMMERS.values().forEach(item -> output.accept(item.get()));
                output.accept(ModItems.LIGHT_ARROW.get());
                output.accept(ModItems.HEAVY_ARROW.get());
                output.accept(ModItems.PADDED_HELMET.get());
                output.accept(ModItems.PADDED_CHESTPLATE.get());
                output.accept(ModItems.PADDED_LEGGINGS.get());
                output.accept(ModItems.PADDED_BOOTS.get());
                output.accept(ModItems.EMBERFALL_MAUL.get());
                output.accept(com.toddlandis.skillcraft.ability.CodexItem.of(net.minecraft.resources.Identifier.fromNamespaceAndPath(com.toddlandis.skillcraft.SkillCraft.MODID, "forgequake")));
                output.accept(ModItems.APPRENTICE_WAND.get());
                output.accept(ModItems.EMBER_STAFF.get());
                output.accept(ModItems.RIME_STAFF.get());
                output.accept(ModItems.TEMPEST_STAFF.get());
                output.accept(ModItems.ARCANIST_WAND.get());
                output.accept(ModItems.STARSEER_STAFF.get());
                output.accept(ModItems.ARCANE_MOTE.get());
                output.accept(ModItems.EMBER_MOTE.get());
                output.accept(ModItems.FROST_MOTE.get());
                output.accept(ModItems.STORM_MOTE.get());
                output.accept(ModItems.VOTIVE_SHRINE.get());
                ModItems.NODES.values().forEach(item -> output.accept(item.get()));
            })
            .build());
}
