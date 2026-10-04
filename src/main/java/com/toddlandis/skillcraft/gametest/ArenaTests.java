package com.toddlandis.skillcraft.gametest;



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
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

// Arena GameTests: the arena can't be broken, built in or blown up; players can enter and leave an
// instance; death and logout always send players home and close the instance.
public final class ArenaTests {
    private ArenaTests() {}

    // The GameTest server only creates the vanilla dimensions, so the arena runs on the Nether as a
    // stand-in there. (A real server loads skillcraft:arena; see dimensionRegistered.)
    private static ServerLevel arena(GameTestHelper helper) {
        ServerLevel arena = ArenaDimension.level(helper.getLevel().getServer());
        if (arena == null) {
            ArenaDimension.useStandInForTests(helper.getLevel().getServer().getLevel(Level.NETHER));
            arena = ArenaDimension.level(helper.getLevel().getServer());
        }
        helper.assertTrue(arena != null, "no arena level available");
        return arena;
    }

    static void dimensionRegistered(GameTestHelper helper) {
        var types = helper.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.DIMENSION_TYPE);
        helper.assertTrue(types.get(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION_TYPE,
                ArenaDimension.LEVEL.identifier())).isPresent(), "dimension type skillcraft:arena did not load");
        helper.succeed();
    }

    static void protection(GameTestHelper helper) {
        ServerLevel arena = arena(helper);
        BlockPos pos = ArenaManager.arenaCenter(900).above(3); // a spot no arena uses
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

        // Entering puts you in the lobby; the boss sleeps until someone steps onto the floor
        helper.assertTrue(manager.enter(player, ArenaManager.SLAGHEART), "could not enter the arena");
        ArenaInstance instance = manager.instance(ArenaManager.SLAGHEART);
        helper.assertTrue(player.level() == arena, "player was not moved into the arena dimension");
        helper.assertTrue(player.position().distanceTo(instance.lobbySpawn()) < 2, "player did not arrive in the lobby");
        helper.assertTrue(manager.isInInstance(player), "player is not a visitor of the arena");
        helper.assertFalse(arena.getBlockState(instance.center().below()).isAir(), "arena floor was not built");
        helper.assertTrue(arena.getBlockState(instance.center().south(com.toddlandis.skillcraft.arena.ArenaBuilder.LOBBY_END - 1))
                .is(com.toddlandis.skillcraft.registry.ModBlocks.EXIT_PORTAL.get()), "exit portal missing from the lobby");

        helper.runAfterDelay(5, () -> {
            helper.assertTrue(instance.state() == ArenaInstance.State.DORMANT, "boss woke while everyone was in the lobby");
            player.teleportTo(arena, instance.center().getX() + 0.5, instance.center().getY(), instance.center().getZ() + 6.5,
                    java.util.Set.of(), 180f, 0f, false);
        });
        helper.runAfterDelay(10, () -> helper.assertTrue(instance.state() == ArenaInstance.State.FIGHTING && instance.boss() != null,
                "boss did not wake when a player stepped onto the floor"));
        // The entity itself can take a while to become findable on a slow (CI) server, so look for
        // it on every tick until tick 160 instead of at one exact tick
        boolean[] bossSeen = {false};
        helper.onEachTick(() -> {
            if (!bossSeen[0] && instance.boss() != null && arena.getEntity(instance.boss()) instanceof SlagheartEntity) bossSeen[0] = true;
        });
        helper.runAfterDelay(160, () -> {
            helper.assertTrue(instance.state() == ArenaInstance.State.FIGHTING, "the fight stopped right after the boss woke");
            helper.assertTrue(bossSeen[0], "boss entity never became findable");
            // Back to the lobby: after RESET_SECONDS with nobody on the floor the boss resets
            Vec3 lobby = instance.lobbySpawn();
            player.teleportTo(arena, lobby.x, lobby.y, lobby.z, java.util.Set.of(), 180f, 0f, false);
        });
        helper.runAfterDelay(165 + ArenaManager.RESET_SECONDS * 20, () -> {
            helper.assertTrue(instance.state() == ArenaInstance.State.DORMANT, "boss did not reset after the arena emptied");
            helper.assertTrue(instance.boss() == null, "reset left a boss behind");
            manager.leave(player, ArenaManager.LeaveReason.EXIT);
            helper.assertTrue(player.level() == helper.getLevel(), "player was not sent back home");
            helper.assertTrue(player.position().distanceTo(start) < 2, "player did not return to where they entered");
            helper.assertFalse(manager.isInInstance(player), "player still listed as a visitor after leaving");
            helper.succeed();
        });
    }

    static void deathAndLogout(GameTestHelper helper) {
        ServerLevel arena = arena(helper);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        ArenaManager manager = ArenaManager.get(helper.getLevel().getServer());

        // Death: cancelled, healed, sent home, attempt lost
        manager.enter(player, ArenaManager.SLAGHEART);
        // The mock player is always in creative and can't take lethal damage, so fire the death
        // event directly: it is exactly what a real death in the arena triggers
        player.setHealth(1f);
        LivingDeathEvent death = new LivingDeathEvent(player, arena.damageSources().generic());
        helper.assertTrue(NeoForge.EVENT_BUS.post(death).isCanceled(), "arena death was not cancelled");
        helper.assertTrue(player.isAlive(), "player died for real in the arena");
        helper.assertTrue(player.getHealth() == player.getMaxHealth(), "player was not healed after an arena death");
        helper.assertTrue(player.level() == helper.getLevel(), "player was not sent home after dying");
        helper.assertFalse(manager.isInInstance(player), "player still listed as a visitor after dying");

        // Logout mid-fight: removed from the arena at once, sent home at the next login
        manager.enter(player, ArenaManager.SLAGHEART);
        NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedOutEvent(player));
        helper.assertFalse(manager.isInInstance(player), "player stayed in the arena after logging out");
        helper.assertTrue(player.level() == arena, "logout should leave the player where they were until they log back in");
        NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedInEvent(player));
        helper.assertTrue(player.level() == helper.getLevel(), "player was not sent home on login");
        helper.succeed();
    }
}
