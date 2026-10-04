package com.toddlandis.skillcraft.gametest;

import java.util.ArrayList;
import java.util.List;

import com.toddlandis.skillcraft.SkillCraft;
import com.toddlandis.skillcraft.altar.minigame.MinigameKind;
import com.toddlandis.skillcraft.altar.minigame.MinigameLogic;
import com.toddlandis.skillcraft.altar.minigame.Minigames;
import com.toddlandis.skillcraft.altar.minigame.SteadyTheCurrentLogic;
import com.toddlandis.skillcraft.leycraft.LeyAltarBlock;
import com.toddlandis.skillcraft.leycraft.LeyAltarBlockEntity;
import com.toddlandis.skillcraft.leycraft.LeyElement;
import com.toddlandis.skillcraft.leycraft.LeyWeaving;
import com.toddlandis.skillcraft.leycraft.Resonance;
import com.toddlandis.skillcraft.node.NodeType;
import com.toddlandis.skillcraft.registry.ModBlocks;
import com.toddlandis.skillcraft.registry.ModDataMaps;
import com.toddlandis.skillcraft.registry.ModItems;
import com.toddlandis.skillcraft.skill.Skill;
import com.toddlandis.skillcraft.skill.SkillManager;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

// GameTests for Leycraft (docs/LEYCRAFT.md): what an altar resonates with, the weave maths
// end to end (quick, careful, level gate, ownership, amethyst at half value), and Steady the
// Current played by an autopilot to show it can be won and replays identically on the server.
public final class LeycraftTests {
    private static final BlockPos ALTAR = new BlockPos(8, 1, 8);

    private LeycraftTests() {}

    // --- Resonance: blocks around the altar decide the element and its strength ---
    static void resonance(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.setBlock(ALTAR, ModBlocks.LEY_ALTAR.get());
        BlockPos altar = helper.absolutePos(ALTAR);

        Resonance.Reading base = Resonance.read(level, altar);
        helper.assertTrue(base.dominant() == LeyElement.ARCANE, "a bare altar should weave Arcane, weaves " + base.dominant() + " " + base.levels());

        // 5 magma blocks = 20 of the 40 points: +50% fire
        place(helper, Blocks.MAGMA_BLOCK, 5, 0);
        Resonance.Reading fire = LeyWeaving.refresh(level, altar);
        double fireGain = fire.of(LeyElement.FIRE) - base.of(LeyElement.FIRE);
        helper.assertTrue(fire.dominant() == LeyElement.FIRE && Math.abs(fireGain - 0.5) < 1e-6, "5 magma blocks should give Fire +50%, gave " + fire.levels());
        helper.assertTrue(level.getBlockState(altar).getValue(LeyAltarBlock.ELEMENT) == LeyElement.FIRE, "the altar block should show Fire after a reading");

        // 6 packed ice = 24 points (60%) out-resonates the fire
        place(helper, Blocks.PACKED_ICE, 6, 1);
        Resonance.Reading frost = Resonance.read(level, altar);
        helper.assertTrue(frost.dominant() == LeyElement.FROST, "more frost than fire should weave Frost, got " + frost.levels());

        // 8 lightning rods = 64 points: storm caps at 100%
        // (Blocks.LIGHTNING_ROD is a set of copper stages in 26.2; take the plain rod by id)
        place(helper, net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("lightning_rod")), 8, 2);
        Resonance.Reading storm = Resonance.read(level, altar);
        helper.assertTrue(storm.dominant() == LeyElement.STORM && storm.strength() == 1.0, "8 lightning rods should cap Storm at 100%, got " + storm.levels());

        SkillCraft.LOGGER.info("[leycraft] resonance: bare {}; +5 magma {}; +6 packed ice {}; +8 lightning rods {}", base.levels(), fire.levels(), frost.levels(), storm.levels());
        helper.succeed();
    }

    // Places `count` blocks in a ring row around the altar (row 0..2 = 2, 3 or 4 blocks away)
    private static void place(GameTestHelper helper, Block block, int count, int row) {
        int d = 2 + row;
        int placed = 0;
        for (int dx = -d; dx <= d && placed < count; dx++) {
            for (int dz = -d; dz <= d && placed < count; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) != d) continue;
                helper.setBlock(ALTAR.offset(dx, 0, dz), block);
                placed++;
            }
        }
    }

    // --- Weaving: yields, XP, level gate, ownership and essence value ---
    static void weaving(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = AbilityTests.player(helper, "skillcraft_weaver", new Vec3(8, 1, 6), 0f);
        ServerPlayer stranger = AbilityTests.player(helper, "skillcraft_stranger", new Vec3(6, 1, 8), 0f);
        player.getInventory().clearContent();
        AbilityTests.level(player, Skill.LEYCRAFT, 1);
        helper.setBlock(ALTAR, ModBlocks.LEY_ALTAR.get());
        BlockPos pos = helper.absolutePos(ALTAR);
        LeyAltarBlockEntity altar = (LeyAltarBlockEntity) level.getBlockEntity(pos);
        double arcaneBase = Resonance.read(level, pos).of(LeyElement.ARCANE);

        // Load by hand: 10 ley shards go on, amethyst is refused while ley shards are loaded
        ItemStack shards = new ItemStack(ModItems.LEY_SHARD.get(), 10);
        use(level, player, pos, shards);
        helper.assertTrue(altar.count() == 10 && shards.isEmpty(), "10 ley shards should load the altar (has " + altar.count() + ")");
        ItemStack amethyst = new ItemStack(Items.AMETHYST_SHARD, 3);
        use(level, player, pos, amethyst);
        helper.assertTrue(amethyst.getCount() == 3 && altar.count() == 10, "a different essence must not mix in");

        // Quick weave, Leycraft 1, bare altar: 4 Arcane Motes per shard (x (1 + arcane resonance)), 6 XP per shard
        int quick = LeyWeaving.quickWeave(player, pos);
        int expectedQuick = (int) Math.round(10 * 4 * (1 + arcaneBase));
        helper.assertTrue(Math.abs(quick - expectedQuick) <= 1 && count(player, ModItems.ARCANE_MOTE.get()) == quick,
                "a quick weave of 10 shards should give about " + expectedQuick + " Arcane Motes, gave " + quick);
        double xp = SkillManager.get(player).getXp(Skill.LEYCRAFT) - com.toddlandis.skillcraft.skill.XpTable.xpForLevel(1);
        helper.assertTrue(Math.abs(xp - 60) < 0.01, "10 shards of Arcane should give 60 Leycraft XP, gave " + xp);
        helper.assertTrue(altar.isEmpty(), "weaving uses up the essence");

        // A full fire ring (10 magma = 100%): Ember needs Leycraft 5
        place(helper, Blocks.MAGMA_BLOCK, 10, 1);
        altar.deposit(player, ModItems.LEY_SHARD.get(), 4);
        helper.assertTrue(LeyWeaving.quickWeave(player, pos) == 0 && altar.count() == 4, "Leycraft 1 must not weave Ember Motes");
        AbilityTests.level(player, Skill.LEYCRAFT, 5);
        helper.assertTrue(LeyWeaving.quickWeave(stranger, pos) == 0 && altar.count() == 4, "only the player who loaded the altar can weave from it");
        int ember = LeyWeaving.quickWeave(player, pos);
        helper.assertTrue(ember == 32, "4 shards at 100% fire should give 4 x 4 x 2 = 32 Ember Motes, gave " + ember);

        // Careful weaves: won with no misses x1.5, lost x0.8, and nothing happens if the element shifted
        altar.deposit(player, ModItems.LEY_SHARD.get(), 8);
        int careful = LeyWeaving.completeCareful(player, pos, LeyElement.FIRE, true, 0);
        helper.assertTrue(careful == 96, "a perfect careful weave of 8 shards should give 8 x 4 x 2 x 1.5 = 96, gave " + careful);
        altar.deposit(player, ModItems.LEY_SHARD.get(), 5);
        int slipped = LeyWeaving.completeCareful(player, pos, LeyElement.FIRE, false, 4);
        helper.assertTrue(slipped == 32, "a lost careful weave of 5 shards should give 5 x 4 x 2 x 0.8 = 32, gave " + slipped);
        altar.deposit(player, ModItems.LEY_SHARD.get(), 2);
        helper.assertTrue(LeyWeaving.completeCareful(player, pos, LeyElement.FROST, true, 0) == 0 && altar.count() == 2,
                "a weave whose element no longer matches must use nothing");
        altar.clear();

        // Amethyst is half an essence
        altar.deposit(player, Items.AMETHYST_SHARD, 6);
        int fromAmethyst = LeyWeaving.quickWeave(player, pos);
        helper.assertTrue(fromAmethyst == 24, "6 amethyst shards at 100% fire should give 6 x 0.5 x 4 x 2 = 24, gave " + fromAmethyst);

        // Where the shards come from: a Ley Stone node, whose heart drops from amethyst clusters
        helper.assertTrue(NodeType.byResource(ModItems.LEY_SHARD.get()) == NodeType.LEY && ModBlocks.NODES.get(NodeType.LEY) != null, "a Ley Stone node should give ley shards");
        var heart = Blocks.AMETHYST_CLUSTER.defaultBlockState().typeHolder().getData(ModDataMaps.BLOCK_HEARTS);
        helper.assertTrue(heart != null && heart.node() == NodeType.LEY, "amethyst clusters should be able to drop a Ley Heart");

        SkillCraft.LOGGER.info("[leycraft] weaves: quick 10 shards bare = {} arcane, 60 XP; 4 shards at 100% fire = {} ember; careful perfect 8 = {}, lost 5 = {}; 6 amethyst = {}",
                quick, ember, careful, slipped, fromAmethyst);
        helper.succeed();
    }

    private static void use(ServerLevel level, ServerPlayer player, BlockPos pos, ItemStack stack) {
        player.setItemSlot(EquipmentSlot.MAINHAND, stack);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        level.getBlockState(pos).useItemOn(stack, level, player, InteractionHand.MAIN_HAND, hit);
    }

    private static int count(ServerPlayer player, Item item) {
        int total = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(item)) total += stack.getCount();
        }
        return total;
    }

    // --- Steady the Current: an autopilot wins it, and the server's replay agrees ---
    static void steadyTheCurrent(GameTestHelper helper) {
        StringBuilder log = new StringBuilder();
        for (int tier = 0; tier <= 2; tier++) {
            for (long seed = 1; seed <= 4; seed++) {
                List<Minigames.Input> inputs = new ArrayList<>();
                SteadyTheCurrentLogic logic = (SteadyTheCurrentLogic) Minigames.create(MinigameKind.STEADY_THE_CURRENT, tier, seed * 7919);
                autopilot(logic, inputs);
                helper.assertTrue(logic.state() == MinigameLogic.State.SUCCESS && logic.score().misses() <= 2,
                        "the autopilot should win tier " + tier + " seed " + seed + " with at most 2 misses (" + logic.state() + ", " + logic.score().misses() + " misses)");
                Minigames.ReplayResult replay = Minigames.replay(MinigameKind.STEADY_THE_CURRENT, tier, seed * 7919, inputs);
                helper.assertTrue(replay.state() == logic.state() && replay.misses() == logic.score().misses(),
                        "the server replay should agree with the client (tier " + tier + " seed " + seed + ": " + replay + ")");
                log.append(" t").append(tier).append("s").append(seed).append("=").append(logic.score().misses()).append("m/").append(replay.ticks()).append("t");
            }
        }
        Minigames.ReplayResult idle = Minigames.replay(MinigameKind.STEADY_THE_CURRENT, 0, 42, List.of());
        helper.assertTrue(idle.state() == MinigameLogic.State.RUNNING && !idle.started(), "a weave closed without touching it must count as not played");
        SkillCraft.LOGGER.info("[leycraft] Steady the Current autopilot (misses/ticks):{}", log);
        helper.succeed();
    }

    // Holds whenever the current, looking a few ticks ahead, is below the band's centre. Records
    // inputs exactly as the client screen does: an input at tick N is applied before tick N runs.
    private static void autopilot(SteadyTheCurrentLogic logic, List<Minigames.Input> inputs) {
        boolean down = false;
        for (int ticks = 0; ticks < Minigames.MAX_TICKS && logic.state() == MinigameLogic.State.RUNNING; ticks++) {
            boolean want = logic.current(1f) + logic.velocity() * 8 < logic.bandCenter(1f);
            if (ticks == 0) want = true; // the first press starts the pulses
            if (want != down) {
                down = want;
                inputs.add(new Minigames.Input(ticks, want));
                if (want) logic.press(); else logic.release();
                if (logic.state() != MinigameLogic.State.RUNNING) break;
            }
            logic.tick();
        }
    }
}
