package com.toddlandis.skillcraft.ability;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

import com.toddlandis.skillcraft.SkillCraft;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

// Ability effects that land a little later (Starfall's star). Each entry runs once after its delay,
// with a warning callback every tick until then. Not saved: a server stop drops pending strikes.
@EventBusSubscriber(modid = SkillCraft.MODID)
public final class DelayedStrikes {
    private record Pending(ServerLevel level, long at, Consumer<ServerLevel> warn, Consumer<ServerLevel> strike) {}

    private static final List<Pending> PENDING = new ArrayList<>();

    private DelayedStrikes() {}

    public static void schedule(ServerLevel level, int delay, Consumer<ServerLevel> warn, Consumer<ServerLevel> strike) {
        PENDING.add(new Pending(level, level.getGameTime() + delay, warn, strike));
    }

    @SubscribeEvent
    static void onServerTick(ServerTickEvent.Post event) {
        if (PENDING.isEmpty()) return;
        List<Pending> due = new ArrayList<>();
        for (Iterator<Pending> it = PENDING.iterator(); it.hasNext(); ) {
            Pending pending = it.next();
            if (pending.level.getGameTime() >= pending.at) {
                due.add(pending);
                it.remove();
            } else {
                pending.warn.accept(pending.level);
            }
        }
        for (Pending pending : due) pending.strike.accept(pending.level);
    }

    // Lets GameTests fire pending strikes without waiting
    public static int pending() {
        return PENDING.size();
    }
}
