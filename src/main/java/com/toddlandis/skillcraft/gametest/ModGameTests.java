package com.toddlandis.skillcraft.gametest;

import java.util.function.Consumer;

import com.toddlandis.skillcraft.SkillCraft;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

// Registers SkillCraft's GameTests (run with ./gradlew runGameTestServer, also in CI).
// Each test gets its own environment, so tests run in separate batches and a boss in one
// test never wanders into another.
@EventBusSubscriber(modid = SkillCraft.MODID)
public final class ModGameTests {
    public static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, SkillCraft.MODID);

    // 16x8x16 stone-floored box: data/skillcraft/structure/test_floor.nbt
    private static final Identifier FLOOR = Identifier.fromNamespaceAndPath(SkillCraft.MODID, "test_floor");

    static {
        TEST_FUNCTIONS.register("arena_dimension", () -> ArenaTests::dimensionRegistered);
        TEST_FUNCTIONS.register("arena_protection", () -> ArenaTests::protection);
        TEST_FUNCTIONS.register("arena_enter_exit", () -> ArenaTests::enterAndExit);
        TEST_FUNCTIONS.register("arena_death_and_logout", () -> ArenaTests::deathAndLogout);
        TEST_FUNCTIONS.register("boss_phases", () -> BossTests::phases);
        TEST_FUNCTIONS.register("boss_stances", () -> BossTests::stances);
        TEST_FUNCTIONS.register("combat_matchups", () -> CombatTests::matchups);
        TEST_FUNCTIONS.register("combat_xp", () -> CombatTests::combatXp);
        TEST_FUNCTIONS.register("constitution", () -> CombatTests::constitution);
        TEST_FUNCTIONS.register("loot_rates", () -> LootRateTests::lootRates);
        TEST_FUNCTIONS.register("ability_validation", () -> AbilityTests::validation);
        TEST_FUNCTIONS.register("ability_momentum", () -> AbilityTests::momentum);
        TEST_FUNCTIONS.register("ability_pipeline", () -> AbilityTests::pipeline);
        TEST_FUNCTIONS.register("ability_boss_daze", () -> AbilityTests::bossDaze);
        TEST_FUNCTIONS.register("ability_wall", () -> AbilityTests::wall);
        TEST_FUNCTIONS.register("ability_loadout", () -> AbilityTests::loadout);
        TEST_FUNCTIONS.register("ability_future", () -> AbilityTests::future);
        TEST_FUNCTIONS.register("ability_offhand", () -> AbilityTests::offhand);
        TEST_FUNCTIONS.register("ability_dps", () -> AbilityTests::dps);
        TEST_FUNCTIONS.register("magic_bolt", () -> Phase7Tests::bolt);
        TEST_FUNCTIONS.register("magic_spells", () -> Phase7Tests::spells);
        TEST_FUNCTIONS.register("prayer", () -> Phase7Tests::prayer);
        TEST_FUNCTIONS.register("agility", () -> Phase7Tests::agility);
    }

    private ModGameTests() {}

    @SubscribeEvent
    static void register(RegisterGameTestsEvent event) {
        test(event, "arena_dimension", 20);
        test(event, "arena_protection", 100);
        test(event, "arena_enter_exit", 500);
        test(event, "arena_death_and_logout", 100);
        test(event, "boss_phases", 400);
        test(event, "boss_stances", 100);
        test(event, "combat_matchups", 40);
        test(event, "combat_xp", 40);
        test(event, "constitution", 40);
        test(event, "loot_rates", 40);
        test(event, "ability_validation", 40);
        test(event, "ability_momentum", 100);
        test(event, "ability_pipeline", 40);
        test(event, "ability_boss_daze", 40);
        test(event, "ability_wall", 40);
        test(event, "ability_loadout", 40);
        test(event, "ability_future", 120);
        test(event, "ability_offhand", 40);
        test(event, "ability_dps", AbilityTests.HARNESS_TICKS + 100);
        test(event, "magic_bolt", 60);
        test(event, "magic_spells", 80);
        test(event, "prayer", 40);
        test(event, "agility", 40);
    }

    private static void test(RegisterGameTestsEvent event, String name, int maxTicks) {
        Identifier id = Identifier.fromNamespaceAndPath(SkillCraft.MODID, name);
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(id);
        event.registerTest(id, new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, id),
                new TestData<>(environment, FLOOR, maxTicks, 1, true)));
    }
}
