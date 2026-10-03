package com.toddlandis.skillcraft.registry;

import java.util.function.Supplier;

import com.toddlandis.skillcraft.SkillCraft;
import com.toddlandis.skillcraft.bank.BankMenu;
import com.toddlandis.skillcraft.bench.BenchMenu;
import com.toddlandis.skillcraft.bench.Station;
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

    public static final Supplier<MenuType<BenchMenu>> FORGING_ANVIL = MENUS.register("forging_anvil",
            () -> new MenuType<>((id, inventory) -> new BenchMenu(Station.ANVIL, id, inventory), FeatureFlags.VANILLA_SET));

    public static final Supplier<MenuType<BenchMenu>> CRAFTING_BENCH = MENUS.register("crafting_bench",
            () -> new MenuType<>((id, inventory) -> new BenchMenu(Station.CRAFTING_BENCH, id, inventory), FeatureFlags.VANILLA_SET));

    public static final Supplier<MenuType<BankMenu>> BANK = MENUS.register("bank",
            () -> new MenuType<>(BankMenu::new, FeatureFlags.VANILLA_SET));
}
