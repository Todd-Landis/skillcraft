package com.toddlandis.skillcraft.arena;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

// Runs boss arena instances. Each fight gets its own plot in the arena dimension, 1024 blocks
// apart along X, rebuilt from scratch when the instance starts. Instances live only in memory:
// after a server restart there are none, any leftover boss removes itself, and players who were
// inside are sent back where they came from when they log in (see ArenaEvents).
public final class ArenaManager {
    public static final int PLOT_SPACING = 1024;
    public static final int ARENA_Y = 64;
    public static final int MAX_PARTY = 4;
    public static final int VICTORY_SECONDS = 90;

    private static MinecraftServer currentServer;
    private static ArenaManager current;

    private final Map<Integer, ArenaInstance> instances = new HashMap<>();
    private final Map<UUID, Integer> byPlayer = new HashMap<>();
    private int nextId = 1;

    private ArenaManager() {}

    public static ArenaManager get(MinecraftServer server) {
        if (currentServer != server) {
            currentServer = server;
            current = new ArenaManager();
        }
        return current;
    }

    public ArenaInstance instance(int id) {
        return instances.get(id);
    }

    public ArenaInstance instanceOf(ServerPlayer player) {
        Integer id = byPlayer.get(player.getUUID());
        return id == null ? null : instances.get(id);
    }

    public int activeCount() {
        return instances.size();
    }

    // Lowest plot number no running instance uses
    private int freePlot() {
        int plot = 0;
        while (true) {
            final int candidate = plot;
            if (instances.values().stream().noneMatch(i -> i.plot() == candidate)) return plot;
            plot++;
        }
    }

    public static BlockPos plotCenter(int plot) {
        return new BlockPos(PLOT_SPACING * (plot + 1), ARENA_Y, 0);
    }

    // Starts a fight for this party. Returns the instance, or empty if the arena isn't available.
    public Optional<ArenaInstance> start(ServerPlayer owner, List<ServerPlayer> party) {
        ServerLevel arena = ArenaDimension.level(owner.level().getServer());
        if (arena == null) {
            SkillCraft.LOGGER.error("Arena dimension {} is missing", ArenaDimension.LEVEL);
            return Optional.empty();
        }
        int plot = freePlot();
        BlockPos center = plotCenter(plot);
        ArenaBuilder.build(arena, center);
        ArenaInstance instance = new ArenaInstance(nextId++, plot, center, owner.getUUID(), party);
        instances.put(instance.id(), instance);

        SlagheartEntity boss = ModEntities.SLAGHEART.get().create(arena, EntitySpawnReason.EVENT);
        if (boss != null) {
            Vec3 spawn = instance.bossSpawn();
            boss.snapTo(spawn.x, spawn.y, spawn.z, 0f, 0f);
            boss.setup(instance.id(), spawn, party.size());
            arena.addFreshEntity(boss);
            instance.setBoss(boss.getUUID());
        }

        for (ServerPlayer player : party) {
            byPlayer.put(player.getUUID(), instance.id());
            player.setData(ModAttachments.ARENA_RETURN, new ArenaReturn(Optional.of(player.level().dimension()), player.position(), player.getYRot(), player.getXRot()));
            Vec3 spawn = instance.playerSpawn();
            player.teleport(new TeleportTransition(arena, spawn, Vec3.ZERO, 180f, 0f, TeleportTransition.DO_NOTHING));
            player.clearFire();
            arena.playSound(null, spawn.x, spawn.y, spawn.z, ModSounds.ARENA_ENTER.get(), SoundSource.PLAYERS, 1f, 1f);
            player.sendSystemMessage(Component.translatable("message.skillcraft.arena_enter").withStyle(ChatFormatting.GOLD));
        }
        SkillCraft.LOGGER.info("Arena instance {} started on plot {} for {} player(s)", instance.id(), plot, party.size());
        return Optional.of(instance);
    }

    public enum LeaveReason { EXIT, DEATH, LOGOUT, ELSEWHERE, VICTORY_TIMEOUT }

    // Takes a player out of their instance. They're sent home unless they are logging out
    // (they'll be sent home when they next log in) or already left the dimension another way.
    public void leave(ServerPlayer player, LeaveReason reason) {
        Integer id = byPlayer.remove(player.getUUID());
        ArenaInstance instance = id == null ? null : instances.get(id);
        if (instance != null) {
            instance.removeMember(player.getUUID());
            if (instance.state() == ArenaInstance.State.VICTORY && reason != LeaveReason.LOGOUT) giveLoot(player, instance);
        }
        if (reason != LeaveReason.LOGOUT && reason != LeaveReason.ELSEWHERE) sendHome(player);
        if (reason == LeaveReason.ELSEWHERE) player.setData(ModAttachments.ARENA_RETURN, ArenaReturn.NONE);
        if (instance != null && instance.members().isEmpty()) close(player.level().getServer(), instance);
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

    // Removes the boss, its adds and any leftovers. The plot is rebuilt when it is next used.
    private void close(MinecraftServer server, ArenaInstance instance) {
        instances.remove(instance.id());
        ServerLevel arena = ArenaDimension.level(server);
        if (arena != null && instance.boss() != null) {
            Entity boss = arena.getEntity(instance.boss());
            if (boss != null) boss.discard();
        }
        SkillCraft.LOGGER.info("Arena instance {} closed", instance.id());
    }

    // Called when the boss dies: each eligible player's loot waits in the reward chest
    public void victory(ServerLevel arena, ArenaInstance instance, Map<UUID, List<ItemStack>> loot) {
        instance.setVictory(loot, arena.getGameTime());
        BlockPos chest = instance.center();
        arena.setBlock(chest, ModBlocks.REWARD_CHEST.get().defaultBlockState(), 3);
        for (UUID id : instance.members()) {
            if (arena.getPlayerByUUID(id) instanceof ServerPlayer player) {
                player.sendSystemMessage(Component.translatable(instance.hasLoot(id) ? "message.skillcraft.arena_victory" : "message.skillcraft.arena_victory_no_loot",
                        VICTORY_SECONDS).withStyle(ChatFormatting.GOLD));
            }
        }
    }

    // Moves a player's loot into their inventory (anything that doesn't fit drops at their feet)
    public void giveLoot(ServerPlayer player, ArenaInstance instance) {
        List<ItemStack> items = instance.takeLoot(player.getUUID());
        for (ItemStack stack : items) {
            if (!player.getInventory().add(stack)) player.drop(stack, false);
        }
        if (!items.isEmpty()) player.sendSystemMessage(Component.translatable("message.skillcraft.arena_loot_claimed", items.size()).withStyle(ChatFormatting.GREEN));
    }

    // Sends everyone home once the post-victory window is over
    public void tick(MinecraftServer server) {
        ServerLevel arena = ArenaDimension.level(server);
        if (arena == null) return;
        for (ArenaInstance instance : new ArrayList<>(instances.values())) {
            if (instance.state() != ArenaInstance.State.VICTORY) continue;
            if (arena.getGameTime() - instance.victoryTick() < VICTORY_SECONDS * 20L) continue;
            for (UUID id : new ArrayList<>(instance.members())) {
                if (arena.getPlayerByUUID(id) instanceof ServerPlayer player) leave(player, LeaveReason.VICTORY_TIMEOUT);
            }
            if (instances.containsKey(instance.id())) close(server, instance);
        }
    }

    // Players still mapped to an instance (for tests and admin checks)
    public boolean isInInstance(ServerPlayer player) {
        return byPlayer.containsKey(player.getUUID());
    }
}
