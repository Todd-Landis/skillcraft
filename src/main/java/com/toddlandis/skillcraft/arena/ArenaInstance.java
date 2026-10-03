package com.toddlandis.skillcraft.arena;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

// One running boss fight: which plot it uses, who is in it, the boss, and (after a win) each
// player's unclaimed loot. Lives only in memory; see ArenaManager.
public final class ArenaInstance {
    public enum State { FIGHTING, VICTORY }

    private final int id;
    private final int plot;
    private final BlockPos center;
    private final UUID owner;
    private final Set<UUID> members = new LinkedHashSet<>();
    private final int partySize;
    private UUID boss;
    private State state = State.FIGHTING;
    private final Map<UUID, List<ItemStack>> loot = new HashMap<>();
    private long victoryTick;

    ArenaInstance(int id, int plot, BlockPos center, UUID owner, List<ServerPlayer> party) {
        this.id = id;
        this.plot = plot;
        this.center = center;
        this.owner = owner;
        for (ServerPlayer player : party) members.add(player.getUUID());
        this.partySize = party.size();
    }

    public int id() {
        return id;
    }

    public int plot() {
        return plot;
    }

    // Centre of the arena floor's top surface (players stand at center.y)
    public BlockPos center() {
        return center;
    }

    public Vec3 bossSpawn() {
        return Vec3.atBottomCenterOf(center.north(2));
    }

    public Vec3 playerSpawn() {
        return Vec3.atBottomCenterOf(center.south(11));
    }

    public UUID owner() {
        return owner;
    }

    public int partySize() {
        return partySize;
    }

    public Set<UUID> members() {
        return members;
    }

    public boolean isMember(ServerPlayer player) {
        return members.contains(player.getUUID());
    }

    void removeMember(UUID player) {
        members.remove(player);
    }

    public UUID boss() {
        return boss;
    }

    void setBoss(UUID boss) {
        this.boss = boss;
    }

    public State state() {
        return state;
    }

    public long victoryTick() {
        return victoryTick;
    }

    void setVictory(Map<UUID, List<ItemStack>> rolled, long tick) {
        state = State.VICTORY;
        victoryTick = tick;
        loot.putAll(rolled);
    }

    public boolean hasLoot(UUID player) {
        return loot.containsKey(player);
    }

    // Removes and returns this player's loot (empty if none or already claimed)
    public List<ItemStack> takeLoot(UUID player) {
        List<ItemStack> items = loot.remove(player);
        return items != null ? items : new ArrayList<>();
    }
}
