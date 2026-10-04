package com.toddlandis.skillcraft.combat;

import net.minecraft.server.level.ServerPlayer;

// Entities that keep a tally of how much damage each player dealt (bosses use it to decide
// who is eligible for loot).
public interface PlayerDamageTracker {
    void recordPlayerDamage(ServerPlayer player, double damage);
}
