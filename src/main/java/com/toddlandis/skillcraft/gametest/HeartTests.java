package com.toddlandis.skillcraft.gametest;

import java.util.ArrayList;
import java.util.List;

import com.toddlandis.skillcraft.SkillCraft;
import com.toddlandis.skillcraft.altar.AttunementAltarBlockEntity;
import com.toddlandis.skillcraft.node.ElementalHeartItem;
import com.toddlandis.skillcraft.node.HeartEvents;
import com.toddlandis.skillcraft.node.NodeType;
import com.toddlandis.skillcraft.registry.ModBlocks;
import com.toddlandis.skillcraft.registry.ModDataMaps;
import com.toddlandis.skillcraft.registry.ModItems;
import com.toddlandis.skillcraft.skill.Skill;
import com.toddlandis.skillcraft.skill.XpTable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

// GameTests for Elemental Hearts and the XP curve (docs/DESIGN.md): the curve is 75% of the OSRS one
// and still doubles every 7 levels; hearts drop from natural logs (part of a tree) and ores at
// their data-map rate, never from blocks a player placed; the altar takes a heart, not resources.
public final class HeartTests {
    private static final int ROLLS = 2000;

    private HeartTests() {}

    static void xpCurve(GameTestHelper helper) {
        long max = XpTable.xpForLevel(99);
        helper.assertTrue(max == 9_775_823L, "level 99 should need 9,775,823 XP (75% of 13,034,431), needs " + max);
        double half = XpTable.xpForLevel(92) / (double) max;
        helper.assertTrue(Math.abs(half - 0.5) < 0.001, "level 92 should still be halfway to 99, is " + half);
        // Same shape as OSRS: x2.7 at 10, x2.18 at 20, then settles to x2.00 from about 40
        for (int level = 30; level <= 92; level++) {
            double ratio = XpTable.xpForLevel(level + 7) / (double) XpTable.xpForLevel(level);
            helper.assertTrue(ratio > 1.95 && ratio < 2.1,"XP should about double every 7 levels; " + level + " -> " + (level + 7) + " is x" + ratio);
        }
        helper.assertTrue(XpTable.levelForXp(max) == 99 && XpTable.levelForXp(max - 1) == 98, "levelForXp should match the table at 99");
        SkillCraft.LOGGER.info("[xp] curve: 99 = {} XP (was 13,034,431), 92 = {} ({}% of 99), 50 = {}, 10 = {}",
                max, XpTable.xpForLevel(92), String.format("%.2f", half * 100), XpTable.xpForLevel(50), XpTable.xpForLevel(10));
        helper.succeed();
    }

    static void hearts(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = AbilityTests.player(helper, "skillcraft_hearts", new Vec3(2, 1, 2), 0f);

        // A grown tree: an oak trunk with natural leaves around the top
        for (int y = 1; y <= 4; y++) helper.setBlock(new BlockPos(4, y, 4), Blocks.OAK_LOG);
        for (int x = 3; x <= 5; x++) for (int z = 3; z <= 5; z++) if (x != 4 || z != 4) helper.setBlock(new BlockPos(x, 4, z), Blocks.OAK_LEAVES);
        helper.setBlock(new BlockPos(4, 5, 4), Blocks.OAK_LEAVES);
        BlockPos trunk = helper.absolutePos(new BlockPos(4, 1, 4));
        helper.assertTrue(HeartEvents.eligible(level, trunk, level.getBlockState(trunk)) != null, "the bottom log of a grown oak should be able to drop an Oak Heart");
        // A lone log (no leaves) and a log next to player-placed leaves are not trees
        helper.setBlock(new BlockPos(11, 1, 11), Blocks.OAK_LOG);
        BlockPos lone = helper.absolutePos(new BlockPos(11, 1, 11));
        helper.assertTrue(HeartEvents.eligible(level, lone, level.getBlockState(lone)) == null, "a lone log is not part of a tree");
        helper.setBlock(new BlockPos(11, 2, 12), Blocks.OAK_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true));
        helper.assertTrue(HeartEvents.eligible(level, lone, level.getBlockState(lone)) == null, "player-placed (persistent) leaves don't make a tree");

        // Ores: the data-map rate over many breaks; a placed ore never drops one
        BlockState diamond = Blocks.DIAMOND_ORE.defaultBlockState();
        double chance = diamond.typeHolder().getData(ModDataMaps.BLOCK_HEARTS).chanceFor(player);
        BlockPos ore = helper.absolutePos(new BlockPos(8, 1, 8));
        helper.setBlock(new BlockPos(8, 1, 8), Blocks.DIAMOND_ORE);
        int natural = 0, placed = 0;
        for (int i = 0; i < ROLLS; i++) natural += breakOnce(level, player, ore, diamond);
        for (int i = 0; i < ROLLS; i++) {
            HeartEvents.markPlaced(level, ore);
            placed += breakOnce(level, player, ore, diamond);
        }
        double expected = ROLLS * chance;
        helper.assertTrue(Math.abs(natural - expected) < 4 * Math.sqrt(expected) + 3,
                "diamond ore should drop a heart about " + expected + " times in " + ROLLS + ", dropped " + natural);
        helper.assertTrue(placed == 0, "a placed diamond ore must never drop a heart, dropped " + placed);
        helper.assertTrue(Items.COD.getDefaultInstance().typeHolder().getData(ModDataMaps.FISHED_HEARTS) != null, "vanilla cod catches should be able to bring up a Cod Heart");

        // The altar takes one heart (and refuses plain resources)
        AbilityTests.level(player, Skill.MINING, 30);
        helper.setBlock(new BlockPos(2, 1, 8), ModBlocks.ATTUNEMENT_ALTAR.get());
        BlockPos altarPos = helper.absolutePos(new BlockPos(2, 1, 8));
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(altarPos), Direction.UP, altarPos, false);
        ItemStack diamonds = new ItemStack(Items.DIAMOND, 16);
        player.setItemSlot(EquipmentSlot.MAINHAND, diamonds);
        level.getBlockState(altarPos).useItemOn(diamonds, level, player, InteractionHand.MAIN_HAND, hit);
        AttunementAltarBlockEntity altar = (AttunementAltarBlockEntity) level.getBlockEntity(altarPos);
        helper.assertTrue(altar.isEmpty() && diamonds.getCount() == 16, "the altar must not take diamonds any more");
        ItemStack hearts = ElementalHeartItem.of(NodeType.DIAMOND);
        hearts.setCount(2);
        player.setItemSlot(EquipmentSlot.MAINHAND, hearts);
        level.getBlockState(altarPos).useItemOn(hearts, level, player, InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(altar.isFull() && altar.target() == NodeType.DIAMOND && hearts.getCount() == 1,
                "one Diamond Heart should fill the altar for a Diamond Node (target " + altar.target() + ", hearts left " + hearts.getCount() + ")");
        ItemStack back = altar.contents();
        helper.assertTrue(ElementalHeartItem.node(back) == NodeType.DIAMOND && back.getCount() == 1 && back.is(ModItems.ELEMENTAL_HEART.get()),
                "withdrawing should give the Diamond Heart back");
        helper.assertTrue(altar.crack(1.0, level.getRandom()) && altar.isEmpty(), "a cracked heart leaves the altar empty");

        SkillCraft.LOGGER.info("[hearts] diamond ore: {} hearts in {} natural breaks (expected {}), {} from {} placed; natural tree yes, lone log no; altar takes 1 heart, refuses diamonds",
                natural, ROLLS, String.format("%.0f", expected), placed, ROLLS);
        helper.succeed();
    }

    // Posts the drops event a real break fires; returns how many hearts it added
    private static int breakOnce(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state) {
        List<ItemEntity> drops = new ArrayList<>();
        drops.add(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, new ItemStack(Items.DIAMOND)));
        NeoForge.EVENT_BUS.post(new BlockDropsEvent(level, pos, state, null, drops, player, new ItemStack(Items.IRON_PICKAXE)));
        int hearts = 0;
        for (ItemEntity entity : drops) if (entity.getItem().is(ModItems.ELEMENTAL_HEART.get())) hearts++;
        return hearts;
    }
}
