package com.toddlandis.skillcraft.gametest;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.toddlandis.skillcraft.SkillCraft;
import com.toddlandis.skillcraft.boss.SlagheartRewards;
import com.toddlandis.skillcraft.loot.CraftedGear;
import com.toddlandis.skillcraft.registry.ModDataComponents;
import com.toddlandis.skillcraft.registry.ModItems;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

// Loot balance harness: rolls Slagheart's reward table many times and logs the average drop per
// kill, the unique rate, and how many "gathering-minutes" one kill is worth (minutes a player at
// the resource's required level needs to gather the same amount at the fatigue-limited rates
// in docs/DESIGN.md). Also checks the gear rules: no ingots, no unstamped gear.
public final class LootRateTests {
    public static final int ROLLS = 20000;

    // Sustained gathering rate per hour at the required level (DESIGN.md gathering fatigue table)
    private static final Map<String, Double> GATHER_PER_HOUR = Map.ofEntries(
            Map.entry("minecraft:coal", 400.0), Map.entry("minecraft:raw_copper", 400.0),
            Map.entry("minecraft:raw_iron", 200.0), Map.entry("minecraft:quartz", 200.0),
            Map.entry("minecraft:raw_gold", 100.0), Map.entry("minecraft:redstone", 100.0), Map.entry("minecraft:lapis_lazuli", 100.0),
            Map.entry("minecraft:dark_oak_log", 100.0), Map.entry("minecraft:cherry_log", 100.0),
            Map.entry("minecraft:pale_oak_log", 40.0), Map.entry("minecraft:diamond", 40.0),
            Map.entry("minecraft:emerald", 12.0), Map.entry("minecraft:ancient_debris", 12.0));

    private LootRateTests() {}

    static void lootRates(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 origin = helper.absoluteVec(new Vec3(8, 1, 8));
        Map<String, Long> totals = new TreeMap<>();
        int uniques = 0, sigils = 0;
        for (int i = 0; i < ROLLS; i++) {
            List<ItemStack> items = SlagheartRewards.roll(helper.getLevel(), player, origin);
            for (ItemStack stack : items) {
                helper.assertFalse(CraftedGear.isSmeltedMaterial(stack), "boss loot contained a smelted material: " + stack);
                helper.assertFalse(CraftedGear.isUnearned(stack), "boss loot contained unstamped gear: " + stack);
                if (stack.is(ModItems.EMBERFALL_MAUL.get())) {
                    uniques++;
                    helper.assertTrue(stack.has(ModDataComponents.EARNED_BY.get()), "the unique was not stamped as earned");
                }
                if (stack.is(ModItems.EMBER_SIGIL.get())) sigils++;
                totals.merge(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), (long) stack.getCount(), Long::sum);
            }
        }

        double gatherMinutes = 0;
        SkillCraft.LOGGER.info("[loot_rates] Slagheart reward table, {} rolls (one roll = one eligible player's kill):", ROLLS);
        for (Map.Entry<String, Long> entry : totals.entrySet()) {
            double perKill = entry.getValue() / (double) ROLLS;
            Double rate = GATHER_PER_HOUR.get(entry.getKey());
            String minutes = rate == null ? "-" : String.format("%.2f", perKill / rate * 60);
            if (rate != null) gatherMinutes += perKill / rate * 60;
            SkillCraft.LOGGER.info("[loot_rates]   {} {}/kill ({} gathering-min)", entry.getKey(), String.format("%.3f", perKill), minutes);
        }
        SkillCraft.LOGGER.info("[loot_rates] unique: {} in {} = 1/{} (target 1/120)", uniques, ROLLS, uniques == 0 ? "inf" : String.format("%.1f", ROLLS / (double) uniques));
        SkillCraft.LOGGER.info("[loot_rates] sigil refund: {} in {} = 1/{} (target 1/20)", sigils, ROLLS, sigils == 0 ? "inf" : String.format("%.1f", ROLLS / (double) sigils));
        SkillCraft.LOGGER.info("[loot_rates] one kill = {} gathering-minutes of resources", String.format("%.2f", gatherMinutes));

        // 20000 rolls at 1/120 -> expect ~167 (sd ~13); allow a wide band
        helper.assertTrue(uniques >= 110 && uniques <= 230, "unique rate off target: " + uniques + " in " + ROLLS);
        helper.assertTrue(sigils >= 800 && sigils <= 1200, "sigil refund rate off target: " + sigils + " in " + ROLLS);
        helper.succeed();
    }
}
