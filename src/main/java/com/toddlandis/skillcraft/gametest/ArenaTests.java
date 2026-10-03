package com.toddlandis.skillcraft.gametest;

import java.util.List;

import com.toddlandis.skillcraft.arena.ArenaDimension;
import com.toddlandis.skillcraft.arena.ArenaInstance;
import com.toddlandis.skillcraft.arena.ArenaManager;
import com.toddlandis.skillcraft.boss.SlagheartEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

// Arena GameTests: the arena can't be broken, built in or blown up; players can enter and leave an
// instance; death and logout always send players home and close the instance.
public final class ArenaTests {
    private ArenaTests() {}

    private static ServerLevel arena(GameTestHelper helper) {
        ServerLevel arena = helper.getLevel().getServer().getLevel(ArenaDimension.LEVEL);
        helper.assertTrue(arena != null, "arena dimension skillcraft:arena is not loaded");
        return arena;
    }

    static void protection(GameTestHelper helper) {
        ServerLevel arena = arena(helper);
        BlockPos pos = ArenaManager.plotCenter(900).above(3);
        arena.getChunk(pos);
        arena.setBlock(pos, Blocks.STONE.defaultBlockState(), 3);

        FakePlayer player = FakePlayerFactory.getMinecraft(arena);
        player.setGameMode(GameType.SURVIVAL);
        player.snapTo(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
        helper.assertFalse(player.gameMode.destroyBlock(pos), "survival player broke a block in the arena");
        helper.assertTrue(arena.getBlockState(pos).is(Blocks.STONE), "arena block is gone after a break attempt");

        BlockEvent.EntityPlaceEvent place = new BlockEvent.EntityPlaceEvent(BlockSnapshot.create(arena.dimension(), arena, pos.above()),
                Blocks.STONE.defaultBlockState(), player);
        helper.assertTrue(NeoForge.EVENT_BUS.post(place).isCanceled(), "block placement in the arena was not cancelled");

        arena.explode(null, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 4f, Level.ExplosionInteraction.TNT);
        helper.assertTrue(arena.getBlockState(pos).is(Blocks.STONE), "an explosion destroyed an arena block");

        arena.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        helper.succeed();
    }

    static void enterAndExit(GameTestHelper helper) {
        ServerLevel arena = arena(helper);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        Vec3 start = player.position();
        ArenaManager manager = ArenaManager.get(helper.getLevel().getServer());

        ArenaInstance instance = manager.start(player, List.of(player)).orElse(null);
        helper.assertTrue(instance != null, "instance did not start");
        helper.assertTrue(player.level() == arena, "player was not moved into the arena");
        helper.assertTrue(manager.isInInstance(player), "player is not a member of the instance");
        helper.assertTrue(arena.getEntity(instance.boss()) instanceof SlagheartEntity, "boss did not spawn");
        BlockPos floor = instance.center().below();
        helper.assertFalse(arena.getBlockState(floor).isAir(), "arena floor was not built");
        helper.assertTrue(arena.getBlockState(instance.center().south(14)).is(com.toddlandis.skillcraft.registry.ModBlocks.EXIT_PORTAL.get()), "exit portal missing");

        helper.runAfterDelay(5, () -> {
            manager.leave(player, ArenaManager.LeaveReason.EXIT);
            helper.assertTrue(player.level() == helper.getLevel(), "player was not sent back home");
            helper.assertTrue(player.position().distanceTo(start) < 2, "player did not return to where they entered");
            helper.assertTrue(manager.instance(instance.id()) == null, "instance was not closed when the last player left");
            helper.assertTrue(arena.getEntity(instance.boss()) == null, "boss was not removed with the instance");
            helper.succeed();
        });
    }

    static void deathAndLogout(GameTestHelper helper) {
        ServerLevel arena = arena(helper);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        ArenaManager manager = ArenaManager.get(helper.getLevel().getServer());

        // Death: cancelled, healed, sent home, attempt lost
        ArenaInstance first = manager.start(player, List.of(player)).orElseThrow();
        player.hurtServer(arena, arena.damageSources().generic(), 1000f);
        helper.assertTrue(player.isAlive(), "player died for real in the arena");
        helper.assertTrue(player.getHealth() == player.getMaxHealth(), "player was not healed after an arena death");
        helper.assertTrue(player.level() == helper.getLevel(), "player was not sent home after dying");
        helper.assertTrue(manager.instance(first.id()) == null, "instance stayed open after its only player died");

        // Logout mid-fight: removed from the instance at once, sent home at the next login
        ArenaInstance second = manager.start(player, List.of(player)).orElseThrow();
        NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedOutEvent(player));
        helper.assertFalse(manager.isInInstance(player), "player stayed in the instance after logging out");
        helper.assertTrue(manager.instance(second.id()) == null, "instance stayed open after its only player logged out");
        helper.assertTrue(player.level() == arena, "logout should leave the player where they were until they log back in");
        NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedInEvent(player));
        helper.assertTrue(player.level() == helper.getLevel(), "player was not sent home on login");
        helper.succeed();
    }
}
