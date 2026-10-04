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

// One boss's permanent arena (there is exactly one per boss, shared by everyone): who is in its
// lobby or arena, the current boss, and (after a kill) each eligible player's unclaimed loot.
// The layout lives at a fixed spot in the arena dimension; the state lives only in memory and is
// rebuilt after a restart. See ArenaManager.
public final class ArenaInstance {
    public enum State { DORMANT, FIGHTING, VICTORY }

    // Arena floor radius; anything farther from the center than this is outside the fight
    public static final double FIGHT_RADIUS = 15.5;

    private final int id;
    private final String name;
    private final BlockPos center;
    private final Set<UUID> visitors = new LinkedHashSet<>();
    private UUID boss;
    private int partySize;
    private State state = State.DORMANT;
    private final Map<UUID, List<ItemStack>> loot = new HashMap<>();
    private long victoryTick;
    private long emptySince = -1;
    private boolean built;

    ArenaInstance(int id, String name, BlockPos center) {
        this.id = id;
        this.name = name;
        this.center = center;
    }

    public int id() { return id; }
    public String name() { return name; }

    // Centre of the arena floor's top surface (players stand at center.y)
    public BlockPos center() { return center; }

    public Vec3 bossSpawn() { return Vec3.atBottomCenterOf(center.north(2)); }

    // Where players arrive in the lobby, facing the corridor to the arena
    public Vec3 lobbySpawn() { return Vec3.atBottomCenterOf(center.south(ArenaBuilder.LOBBY_SPAWN)); }

    public Set<UUID> visitors() { return visitors; }

    public boolean isVisitor(ServerPlayer player) { return visitors.contains(player.getUUID()); }

    void addVisitor(UUID player) { visitors.add(player); }

    void removeVisitor(UUID player) { visitors.remove(player); }

    // A visitor standing on the arena floor (not in the corridor or lobby)
    public boolean isFighting(ServerPlayer player) {
        if (!isVisitor(player) || !ArenaDimension.is(player.level())) return false;
        double dx = player.getX() - (center.getX() + 0.5), dz = player.getZ() - (center.getZ() + 0.5);
        double dy = player.getY() - center.getY();
        return dx * dx + dz * dz <= FIGHT_RADIUS * FIGHT_RADIUS && dy > -3 && dy < ArenaBuilder.HEIGHT;
    }

    public UUID boss() { return boss; }
    void setBoss(UUID boss) { this.boss = boss; }

    public int partySize() { return partySize; }
    void setPartySize(int partySize) { this.partySize = partySize; }

    public State state() { return state; }
    void setState(State state) { this.state = state; }

    public long victoryTick() { return victoryTick; }

    long emptySince() { return emptySince; }
    void setEmptySince(long tick) { emptySince = tick; }

    // When the boss entity was last found missing (-1 = it's there). A freshly spawned boss can
    // take a few ticks to become findable on a busy server, so it only counts as lost after a while.
    private long bossMissingSince = -1;
    long bossMissingSince() { return bossMissingSince; }
    void setBossMissingSince(long tick) { bossMissingSince = tick; }

    boolean built() { return built; }
    void setBuilt(boolean built) { this.built = built; }

    void setVictory(Map<UUID, List<ItemStack>> rolled, long tick) {
        state = State.VICTORY;
        victoryTick = tick;
        loot.clear();
        loot.putAll(rolled);
    }

    public boolean hasLoot(UUID player) { return loot.containsKey(player); }

    Set<UUID> playersWithLoot() { return Set.copyOf(loot.keySet()); }

    // Removes and returns this player's loot (empty if none or already claimed)
    public List<ItemStack> takeLoot(UUID player) {
        List<ItemStack> items = loot.remove(player);
        return items != null ? items : new ArrayList<>();
    }

    void clearLoot() { loot.clear(); }
}
