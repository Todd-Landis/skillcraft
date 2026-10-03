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
                output.accept(ModItems.SMELTER.get());
                output.accept(ModItems.FORGING_ANVIL.get());
                output.accept(ModItems.CRAFTING_BENCH.get());
                ModItems.NODES.values().forEach(item -> output.accept(item.get()));
            })
            .build());
}
