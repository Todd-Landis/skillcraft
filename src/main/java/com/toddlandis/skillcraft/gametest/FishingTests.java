package com.toddlandis.skillcraft.gametest;

import java.util.ArrayList;
import java.util.List;

import com.toddlandis.skillcraft.SkillCraft;
import com.toddlandis.skillcraft.altar.minigame.MinigameKind;
import com.toddlandis.skillcraft.altar.minigame.MinigameLogic;
import com.toddlandis.skillcraft.altar.minigame.Minigames;
import com.toddlandis.skillcraft.altar.minigame.SetTheHookLogic;
import com.toddlandis.skillcraft.fishing.FishingSessions;
import com.toddlandis.skillcraft.fishing.FishingSpotEntity;
import com.toddlandis.skillcraft.fishing.FishingWater;
import com.toddlandis.skillcraft.node.NodeType;
import com.toddlandis.skillcraft.skill.Skill;
import com.toddlandis.skillcraft.skill.SkillManager;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

// Fishing: where spots may go, a full catch -> swim off -> resurface cycle, the vanilla-fishing
// level filter, and the Set the Hook minigame.
final class FishingTests {
    private FishingTests() {}

    // Fills [x0..x1] x [z0..z1] with water `depth` deep (from y=1 up) inside a stone rim
    static void pond(GameTestHelper helper, int x0, int z0, int x1, int z1, int depth) {
        for (int x = x0 - 1; x <= x1 + 1; x++) {
            for (int z = z0 - 1; z <= z1 + 1; z++) {
                boolean inside = x >= x0 && x <= x1 && z >= z0 && z <= z1;
                for (int y = 1; y <= 2; y++) {
                    var state = !inside ? Blocks.STONE.defaultBlockState()
                            : y <= depth ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState();
                    helper.setBlock(new BlockPos(x, y, z), state);
                }
            }
        }
    }

    static void water(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        // A single block of water, a 1-wide channel, a shallow pond and a proper 5x5 pond
        pond(helper, 1, 1, 1, 1, 2);
        pond(helper, 4, 1, 4, 12, 2);
        pond(helper, 7, 1, 11, 5, 1);
        pond(helper, 7, 8, 11, 12, 2);

        BlockPos single = helper.absolutePos(new BlockPos(1, 2, 1));
        BlockPos channel = helper.absolutePos(new BlockPos(4, 2, 6));
        BlockPos shallow = helper.absolutePos(new BlockPos(9, 1, 3));
        BlockPos pond = helper.absolutePos(new BlockPos(9, 2, 10));
        int cod = NodeType.COD.minWaterArea();

        helper.assertTrue(FishingWater.problem(level, single, cod) != null, "a single block of water accepted a shoal");
        helper.assertTrue("message.skillcraft.shoal_too_narrow".equals(FishingWater.problem(level, channel, cod)),
                "a 1-wide channel should be too narrow, got " + FishingWater.problem(level, channel, cod));
        helper.assertTrue("message.skillcraft.shoal_too_shallow".equals(FishingWater.problem(level, shallow, cod)),
                "1-deep water should be too shallow, got " + FishingWater.problem(level, shallow, cod));
        helper.assertTrue(FishingWater.problem(level, pond, cod) == null, "a 5x5, 2-deep pond rejected a cod shoal: " + FishingWater.problem(level, pond, cod));
        helper.assertTrue(FishingWater.isValid(level, pond, NodeType.TROPICAL_FISH.minWaterArea()), "a 25-tile pond should take a reef fish shoal (24)");
        helper.assertTrue("message.skillcraft.shoal_too_small".equals(FishingWater.problem(level, pond, NodeType.PUFFERFISH.minWaterArea())),
                "a 25-tile pond should be too small for pufferfish (30)");
        helper.assertTrue(FishingWater.problem(level, pond.below(), cod) != null, "a spot under the surface was accepted");
        SkillCraft.LOGGER.info("[fishing] water: single/channel/shallow rejected, 25-tile pond takes tiers I-III (12/18/24), not IV (30)");
        helper.succeed();
    }

    // Cast at a cod spot at level 1: the first catch always depletes it (depletion chance is 100%
    // at the required level), then it resurfaces on another tile after the 20 s regrow.
    static void session(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        pond(helper, 3, 3, 11, 11, 2);
        BlockPos water = helper.absolutePos(new BlockPos(7, 2, 5));
        FishingSpotEntity spot = FishingSpotEntity.create(level, NodeType.COD, water);
        level.addFreshEntity(spot);

        ServerPlayer player = AbilityTests.player(helper, "skillcraft_fishing_session", new Vec3(7.5, 3, 1.5), 0f);
        player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.FISHING_ROD));
        player.getInventory().clearContent();
        player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.FISHING_ROD));

        FishingSessions.start(player, spot);
        helper.assertTrue(FishingSessions.isFishing(player), "right-clicking the spot with a rod did not start fishing");
        helper.assertTrue(player.fishing instanceof com.toddlandis.skillcraft.fishing.FishingLine, "no line was cast to the spot");

        long[] caughtAt = {-1};
        boolean[] pickableWhileGone = {false};
        BlockPos[] firstTile = {spot.waterPos()};
        helper.onEachTick(() -> {
            FishingSessions.tick(player);
            if (caughtAt[0] < 0 && spot.isDepleted()) {
                caughtAt[0] = level.getGameTime();
                pickableWhileGone[0] = spot.isPickable();
            }
        });
        int regrow = NodeType.COD.respawnTicks();
        helper.runAfterDelay(600, () -> {
            helper.assertTrue(caughtAt[0] >= 0, "no fish caught in 30 s at 40% per attempt");
            helper.assertTrue(player.getInventory().countItem(Items.COD) == 1, "expected exactly 1 cod, have " + player.getInventory().countItem(Items.COD));
            double xp = SkillManager.get(player).getXp(Skill.FISHING);
            helper.assertTrue(Math.abs(xp - NodeType.COD.xp()) < 0.01, "expected " + NodeType.COD.xp() + " Fishing XP, have " + xp);
            helper.assertFalse(FishingSessions.isFishing(player), "fishing should stop when the shoal swims off");
            helper.assertTrue(player.fishing == null, "the line should be reeled in when fishing stops");
            helper.assertFalse(pickableWhileGone[0], "a shoal that swam off can still be clicked");
        });
        helper.runAfterDelay(600 + regrow + 20, () -> {
            helper.assertFalse(spot.isDepleted(), "the shoal did not resurface after the regrow time");
            helper.assertFalse(spot.waterPos().equals(firstTile[0]), "the shoal resurfaced on the same tile instead of moving");
            helper.assertTrue(FishingWater.isValid(level, spot.waterPos(), NodeType.COD.minWaterArea()), "the shoal resurfaced somewhere invalid");
            helper.assertTrue(FishingWater.nearShore(level, spot.waterPos()), "the shoal resurfaced out of reach of the bank");
            SkillCraft.LOGGER.info("[fishing] session: caught 1 cod (+{} XP), shoal swam off and resurfaced {} -> {} after {} s",
                    NodeType.COD.xp(), firstTile[0], spot.waterPos(), regrow / 20);
            helper.succeed();
        });
    }

    // Vanilla fishing still gives fish, but never ones above your Fishing level
    static void vanillaCatches(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = AbilityTests.player(helper, "skillcraft_vanilla_fishing", new Vec3(4, 1, 4), 0f);
        ItemStack rod = new ItemStack(Items.FISHING_ROD);
        player.setItemSlot(EquipmentSlot.MAINHAND, rod);
        FishingHook hook = new FishingHook(player, level, 0, 0);
        LootTable table = level.getServer().reloadableRegistries().getLootTable(BuiltInLootTables.FISHING);

        int[] lowLevel = roll(level, player, hook, rod, table);
        AbilityTests.level(player, Skill.FISHING, 99);
        int[] highLevel = roll(level, player, hook, rod, table);
        hook.discard();

        helper.assertTrue(lowLevel[0] > 0, "level 1 anglers should still catch cod");
        helper.assertTrue(lowLevel[1] == 0 && lowLevel[2] == 0 && lowLevel[3] == 0,
                "level 1 caught fish above their level: salmon " + lowLevel[1] + ", reef " + lowLevel[2] + ", puffer " + lowLevel[3]);
        helper.assertTrue(highLevel[1] > 0, "level 99 anglers should catch salmon");
        SkillCraft.LOGGER.info("[fishing] vanilla, 400 rolls: level 1 cod/salmon/reef/puffer {}/{}/{}/{}; level 99 {}/{}/{}/{}",
                lowLevel[0], lowLevel[1], lowLevel[2], lowLevel[3], highLevel[0], highLevel[1], highLevel[2], highLevel[3]);
        helper.succeed();
    }

    private static int[] roll(ServerLevel level, ServerPlayer player, FishingHook hook, ItemStack rod, LootTable table) {
        int[] counts = new int[4];
        for (int i = 0; i < 400; i++) {
            LootParams params = new LootParams.Builder(level)
                    .withParameter(LootContextParams.ORIGIN, hook.position())
                    .withParameter(LootContextParams.TOOL, rod)
                    .withParameter(LootContextParams.THIS_ENTITY, hook)
                    .withParameter(LootContextParams.ATTACKING_ENTITY, player)
                    .create(LootContextParamSets.FISHING);
            for (ItemStack stack : table.getRandomItems(params)) {
                if (stack.is(Items.COD)) counts[0]++;
                if (stack.is(Items.SALMON)) counts[1]++;
                if (stack.is(Items.TROPICAL_FISH)) counts[2]++;
                if (stack.is(Items.PUFFERFISH)) counts[3]++;
            }
        }
        return counts;
    }

    // A perfect player (strikes only on bites) wins; striking at a nibble is a miss; the server
    // replay of the recorded presses agrees with the live game.
    static void hookMinigame(GameTestHelper helper) {
        for (int tier = 0; tier < 4; tier++) {
            long seed = 1234L + tier;
            SetTheHookLogic live = new SetTheHookLogic(tier, seed);
            List<Minigames.Input> inputs = new ArrayList<>();
            int nibbles = 0;
            for (int tick = 0; tick < Minigames.MAX_TICKS && live.state() == MinigameLogic.State.RUNNING; tick++) {
                if (live.phase() == SetTheHookLogic.Phase.BITE && live.biteProgress() > 0.3f) {
                    inputs.add(new Minigames.Input(tick, true));
                    live.press();
                    if (live.state() != MinigameLogic.State.RUNNING) break;
                }
                if (live.phase() == SetTheHookLogic.Phase.NIBBLE) nibbles++;
                live.tick();
            }
            helper.assertTrue(live.state() == MinigameLogic.State.SUCCESS, "tier " + tier + ": striking every bite should win, got " + live.state());
            Minigames.ReplayResult replay = Minigames.replay(MinigameKind.SET_THE_HOOK, tier, seed, inputs);
            helper.assertTrue(replay.state() == MinigameLogic.State.SUCCESS, "tier " + tier + ": server replay disagreed: " + replay.state());
            if (tier >= 2) helper.assertTrue(nibbles > 0, "tier " + tier + " had no nibbles to fake the player out");
        }

        // Striking at a nibble is a miss
        SetTheHookLogic nibble = new SetTheHookLogic(3, 99L);
        for (int i = 0; i < Minigames.MAX_TICKS && nibble.phase() != SetTheHookLogic.Phase.NIBBLE; i++) nibble.tick();
        helper.assertTrue(nibble.phase() == SetTheHookLogic.Phase.NIBBLE, "no nibble happened at tier 3");
        nibble.press();
        helper.assertTrue(nibble.score().misses() == 1, "striking a nibble should be a miss");

        // Idle with no presses: bites pass without penalty, so closing without playing costs nothing
        SetTheHookLogic idle = new SetTheHookLogic(0, 5L);
        for (int i = 0; i < 600; i++) idle.tick();
        helper.assertTrue(idle.score().misses() == 0 && !idle.started(), "an idle game should not count misses");
        helper.succeed();
    }
}
