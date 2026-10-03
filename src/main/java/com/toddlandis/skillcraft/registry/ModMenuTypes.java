package com.toddlandis.skillcraft.registry;

import java.util.function.Supplier;

import com.toddlandis.skillcraft.SkillCraft;
import com.toddlandis.skillcraft.smithing.SmelterMenu;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredRegister;

// Container screens (menus) for stations
public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, SkillCraft.MODID);

    public static final Supplier<MenuType<SmelterMenu>> SMELTER = MENUS.register("smelter",
            () -> new MenuType<>(SmelterMenu::new, FeatureFlags.VANILLA_SET));
}
