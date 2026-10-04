package com.toddlandis.skillcraft.arena;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.toddlandis.skillcraft.SkillCraft;
import com.toddlandis.skillcraft.boss.SlagheartEntity;
import com.toddlandis.skillcraft.registry.ModAttachments;
import com.toddlandis.skillcraft.registry.ModBlocks;
import com.toddlandis.skillcraft.registry.ModEntities;
import com.toddlandis.skillcraft.registry.ModSounds;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

// Runs the boss arenas. Every boss has ONE permanent arena in the arena dimension, shared by
// everyone, at a fixed spot far from the others (so no arena can be reached from another):
//   - The Summoning Brazier takes you to the arena's lobby (the Ember Sigil is the entry fee).
//   - The boss wakes as soon as anyone steps onto the arena floor; everyone on the floor fights it.
//   - After a kill the reward chest appears and the boss reforms RESPAWN_SECONDS later.
//   - If nobody has been on the floor for RESET_SECONDS, the boss is removed and its hazards
//     cleared, ready to wake fresh for the next group.
// The layout is rebuilt at the first visit after each server start; all fight state lives in
// memory. After a restart any leftover boss removes itself and players who were inside are sent
// back where they came from when they log in (see ArenaEvents).
public final class ArenaManager {
    public static final int ARENA_Y = 64;
    public static final int RESPAWN_SECONDS = 60;
    public static final int RESET_SECONDS = 10;
    private static final int BOSS_LOST_TICKS = 40;

    // Arena ids. Future bosses get their own entry and spot (2048 blocks apart along X).
    public static final int SLAGHEART = 0;

    private static MinecraftServer currentServer;
    private static ArenaManager current;

    private final List<ArenaInstance> arenas = List.of(
            new ArenaInstance(SLAGHEART, "slagheart", arenaCenter(SLAGHEART)));

    private ArenaManager() {}

    public static ArenaManager get(MinecraftServer server) {
        if (currentServer != server) {
            currentServer = server;
            current = new ArenaManager();
        }
        return current;
    }

    public static BlockPos arenaCenter(int id) {
        return new BlockPos(2048 * (id + 1), ARENA_Y, 0);
    }

    public ArenaInstance instance(int id) {
        return id >= 0 && id < arenas.size() ? arenas.get(id) : null;
    }

    public ArenaInstance instanceOf(ServerPlayer player) {
        for (ArenaInstance arena : arenas) if (arena.isVisitor(player)) return arena;
        return null;
    }

    public boolean isInInstance(ServerPlayer player) {
        return instanceOf(player) != null;
    }

    // --- entering and leaving ---

    // Sends a player to an arena's lobby. Returns false if the arena dimension isn't available.
    public boolean enter(ServerPlayer player, int arenaId) {
        ServerLevel level = ArenaDimension.level(player.level().getServer());
        ArenaInstance arena = instance(arenaId);
        if (level == null || arena == null) {
            SkillCraft.LOGGER.error("Arena dimension {} is missing", ArenaDimension.LEVEL);
            return false;
        }
        ensureBuilt(level, arena);
        arena.addVisitor(player.getUUID());
        player.setData(ModAttachments.ARENA_RETURN, new ArenaReturn(java.util.Optional.of(player.level().dimension()), player.position(), player.getYRot(), player.getXRot()));
        Vec3 spawn = arena.lobbySpawn();
        player.teleport(new TeleportTransition(level, spawn, Vec3.ZERO, 180f, 0f, TeleportTransition.DO_NOTHING));
        player.clearFire();
        level.playSound(null, spawn.x, spawn.y, spawn.z, ModSounds.ARENA_ENTER.get(), SoundSource.PLAYERS, 1f, 1f);
        player.sendSystemMessage(Component.translatable("message.skillcraft.arena_lobby").withStyle(ChatFormatting.GOLD));
        return true;
    }

    public enum LeaveReason { EXIT, DEATH, LOGOUT, ELSEWHERE }

    // Takes a player out of the arena. They're sent home unless they are logging out (they'll be
    // sent home when they next log in) or already left the dimension another way. Unclaimed loot
    // from the last kill goes with them.
    public void leave(ServerPlayer player, LeaveReason reason) {
        ArenaInstance arena = instanceOf(player);
        if (arena != null) {
            arena.removeVisitor(player.getUUID());
            if (arena.hasLoot(player.getUUID()) && reason != LeaveReason.LOGOUT) giveLoot(player, arena);
        }
        if (reason != LeaveReason.LOGOUT && reason != LeaveReason.ELSEWHERE) sendHome(player);
        if (reason == LeaveReason.ELSEWHERE) player.setData(ModAttachments.ARENA_RETURN, ArenaReturn.NONE);
    }

    // Sends a player back to where they entered the arena from (or to world spawn)
    public static void sendHome(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        ArenaReturn back = player.getData(ModAttachments.ARENA_RETURN);
        ServerLevel target = back.dimension().map(server::getLevel).orElse(null);
        player.setData(ModAttachments.ARENA_RETURN, ArenaReturn.NONE);
        TeleportTransition transition = target != null && !ArenaDimension.is(target)
                ? new TeleportTransition(target, back.pos(), Vec3.ZERO, back.yRot(), back.xRot(), TeleportTransition.DO_NOTHING)
                : TeleportTransition.createDefault(player, TeleportTransition.DO_NOTHING);
        player.teleport(transition);
    }

    // --- the fight loop ---

    public void tick(MinecraftServer server) {
        ServerLevel level = ArenaDimension.level(server);
        if (level == null) return;
        long now = level.getGameTime();
        for (ArenaInstance arena : arenas) {
            if (arena.visitors().isEmpty() && arena.state() != ArenaInstance.State.FIGHTING) continue;
            List<ServerPlayer> fighters = fighters(level, arena);
            switch (arena.state()) {
                case DORMANT -> {
                    if (!fighters.isEmpty()) spawnBoss(level, arena, fighters.size());
                }
                case FIGHTING -> {
                    Entity boss = arena.boss() == null ? null : level.getEntity(arena.boss());
                    if (fighters.isEmpty()) {
                        if (arena.emptySince() < 0) arena.setEmptySince(now);
                        else if (now - arena.emptySince() >= RESET_SECONDS * 20L) reset(level, arena, boss);
                    } else {
                        arena.setEmptySince(-1);
                        if (boss != null && boss.isAlive()) {
                            arena.setBossMissingSince(-1);
                        } else if (arena.bossMissingSince() < 0) {
                            arena.setBossMissingSince(now);
                        } else if (now - arena.bossMissingSince() >= BOSS_LOST_TICKS) {
                            reset(level, arena, boss); // lost track of it for a while: start over
                        }
                    }
                }
                case VICTORY -> {
                    if (now - arena.victoryTick() >= RESPAWN_SECONDS * 20L) {
                        // Anyone who didn't open the chest gets their loot now; then the boss reforms
                        for (UUID id : arena.playersWithLoot()) {
                            if (level.getServer().getPlayerList().getPlayer(id) instanceof ServerPlayer player && arena.isVisitor(player)) giveLoot(player, arena);
                        }
                        arena.clearLoot();
                        level.setBlock(arena.center(), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                        arena.setState(ArenaInstance.State.DORMANT);
                        message(level, arena, "message.skillcraft.arena_reformed");
                    }
                }
            }
            if (now % 10 == 0) keepMobsInArena(level, arena);
            // Keep the arena simulated (boss and adds ticking) while anyone is here or a fight is on.
            // The ticket expires by itself (15 s) once this stops renewing it.
            if (now % 20 == 0) {
                level.getChunkSource().addTicketWithRadius(net.minecraft.server.level.TicketType.PORTAL,
                        net.minecraft.world.level.ChunkPos.containing(arena.center()), 3);
            }
        }
    }

    // Visitors standing on the arena floor
    public List<ServerPlayer> fighters(ServerLevel level, ArenaInstance arena) {
        List<ServerPlayer> fighters = new ArrayList<>();
        for (ServerPlayer player : level.players()) {
            if (player.isAlive() && !player.isSpectator() && arena.isFighting(player)) fighters.add(player);
        }
        return fighters;
    }

    private void spawnBoss(ServerLevel level, ArenaInstance arena, int partySize) {
        ArenaBuilder.build(level, arena.center()); // cheap: only changed blocks are replaced
        SlagheartEntity boss = ModEntities.SLAGHEART.get().create(level, EntitySpawnReason.EVENT);
        if (boss == null) return;
        Vec3 spawn = arena.bossSpawn();
        boss.snapTo(spawn.x, spawn.y, spawn.z, 0f, 0f);
        level.getChunkAt(BlockPos.containing(spawn)); // make sure the boss lands in a loaded chunk
        level.getChunkSource().addTicketWithRadius(net.minecraft.server.level.TicketType.PORTAL,
                net.minecraft.world.level.ChunkPos.containing(arena.center()), 3); // ...that is simulated
        boss.setup(arena.id(), spawn, partySize);
        if (!level.addFreshEntity(boss)) {
            SkillCraft.LOGGER.warn("Arena {}: the boss could not be spawned", arena.name());
            return;
        }
        arena.setBoss(boss.getUUID());
        arena.setPartySize(partySize);
        arena.setEmptySince(-1);
        arena.setBossMissingSince(-1);
        arena.setState(ArenaInstance.State.FIGHTING);
        SkillCraft.LOGGER.info("Arena {}: boss awakened for {} player(s)", arena.name(), partySize);
    }

    // Removes the boss (if loaded) and its leftovers; the next visitor wakes a fresh one
    private void reset(ServerLevel level, ArenaInstance arena, Entity boss) {
        if (boss != null) boss.discard(); // the boss restores its hazard tiles and removes its adds when discarded
        arena.setBoss(null);
        arena.setEmptySince(-1);
        arena.setBossMissingSince(-1);
        arena.setState(ArenaInstance.State.DORMANT);
        SkillCraft.LOGGER.info("Arena {}: reset", arena.name());
    }

    // The boss and its adds stay on the arena floor; anything that wanders off is put back
    private void keepMobsInArena(ServerLevel level, ArenaInstance arena) {
        BlockPos c = arena.center();
        AABB outside = new AABB(c.getX() - 12, c.getY() - 2, c.getZ() + ArenaInstance.FIGHT_RADIUS,
                c.getX() + 13, c.getY() + 12, c.getZ() + ArenaBuilder.LOBBY_END + 2);
        Vec3 home = arena.bossSpawn();
        for (Mob mob : level.getEntitiesOfClass(Mob.class, outside)) mob.teleportTo(home.x, home.y, home.z);
    }

    // Called when the boss dies: each eligible player's loot waits in the reward chest
    public void victory(ServerLevel level, ArenaInstance arena, Map<UUID, List<ItemStack>> loot) {
        arena.setVictory(loot, level.getGameTime());
        level.setBlock(arena.center(), ModBlocks.REWARD_CHEST.get().defaultBlockState(), 3);
        for (UUID id : arena.visitors()) {
            if (level.getPlayerByUUID(id) instanceof ServerPlayer player) {
                player.sendSystemMessage(Component.translatable(arena.hasLoot(id) ? "message.skillcraft.arena_victory" : "message.skillcraft.arena_victory_no_loot",
                        RESPAWN_SECONDS).withStyle(ChatFormatting.GOLD));
            }
        }
    }

    // Moves a player's loot into their inventory (anything that doesn't fit drops at their feet)
    public void giveLoot(ServerPlayer player, ArenaInstance arena) {
        List<ItemStack> items = arena.takeLoot(player.getUUID());
        for (ItemStack stack : items) {
            if (!player.getInventory().add(stack)) player.drop(stack, false);
        }
        if (!items.isEmpty()) player.sendSystemMessage(Component.translatable("message.skillcraft.arena_loot_claimed", items.size()).withStyle(ChatFormatting.GREEN));
    }

    private void message(ServerLevel level, ArenaInstance arena, String key) {
        for (UUID id : arena.visitors()) {
            if (level.getPlayerByUUID(id) instanceof ServerPlayer player) player.sendSystemMessage(Component.translatable(key).withStyle(ChatFormatting.GOLD));
        }
    }

    // Builds the arena and lobby once per server session (and whenever the layout changes)
    private void ensureBuilt(ServerLevel level, ArenaInstance arena) {
        if (arena.built()) return;
        ArenaBuilder.build(level, arena.center());
        arena.setBuilt(true);
        SkillCraft.LOGGER.info("Arena {} built at {}", arena.name(), arena.center());
    }
}
