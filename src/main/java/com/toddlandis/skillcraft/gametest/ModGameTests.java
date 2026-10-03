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
        TEST_FUNCTIONS.register("arena_protection", () -> ArenaTests::protection);
        TEST_FUNCTIONS.register("arena_enter_exit", () -> ArenaTests::enterAndExit);
        TEST_FUNCTIONS.register("arena_death_and_logout", () -> ArenaTests::deathAndLogout);
        TEST_FUNCTIONS.register("boss_phases", () -> BossTests::phases);
        TEST_FUNCTIONS.register("boss_stances", () -> BossTests::stances);
        TEST_FUNCTIONS.register("combat_matchups", () -> CombatTests::matchups);
        TEST_FUNCTIONS.register("combat_xp", () -> CombatTests::combatXp);
        TEST_FUNCTIONS.register("constitution", () -> CombatTests::constitution);
        TEST_FUNCTIONS.register("loot_rates", () -> LootRateTests::lootRates);
    }

    private ModGameTests() {}

    @SubscribeEvent
    static void register(RegisterGameTestsEvent event) {
        test(event, "arena_protection", 100);
        test(event, "arena_enter_exit", 100);
        test(event, "arena_death_and_logout", 100);
        test(event, "boss_phases", 400);
        test(event, "boss_stances", 100);
        test(event, "combat_matchups", 40);
        test(event, "combat_xp", 40);
        test(event, "constitution", 40);
        test(event, "loot_rates", 40);
    }

    private static void test(RegisterGameTestsEvent event, String name, int maxTicks) {
        Identifier id = Identifier.fromNamespaceAndPath(SkillCraft.MODID, name);
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(id);
        event.registerTest(id, new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, id),
                new TestData<>(environment, FLOOR, maxTicks, 1, true)));
    }
}
