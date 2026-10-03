package com.toddlandis.skillcraft.node;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

// Per-player record of recently gathered resources, one meter per node type.
// Each successful gather adds 1; the meter drains continuously at the player's sustained rate
// (NodeType.fatiguePerHour). When it reaches NodeType.fatigueCapacity the player can't gather
// that resource until it drains. This caps how fast any one player can gather a resource no
// matter how many nodes exist or who placed them. Stored on the player (ModAttachments.FATIGUE).
public class GatheringFatigue {
    private record Meter(double amount, long tick) {
        static final Codec<Meter> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.DOUBLE.fieldOf("amount").forGetter(Meter::amount),
                Codec.LONG.fieldOf("tick").forGetter(Meter::tick)
        ).apply(instance, Meter::new));
    }

    public static final MapCodec<GatheringFatigue> CODEC = Codec.unboundedMap(NodeType.CODEC, Meter.CODEC)
            .xmap(GatheringFatigue::new, fatigue -> new HashMap<>(fatigue.meters))
            .fieldOf("meters");

    private final Map<NodeType, Meter> meters = new EnumMap<>(NodeType.class);

    public GatheringFatigue() {}

    private GatheringFatigue(Map<NodeType, Meter> saved) {
        meters.putAll(saved);
    }

    // Current fatigue after draining since the last update
    public double current(NodeType type, int playerLevel, long now) {
        Meter meter = meters.get(type);
        if (meter == null) return 0;
        double drained = drainPerTick(type, playerLevel) * Math.max(0, now - meter.tick());
        return Math.max(0, meter.amount() - drained);
    }

    public void add(NodeType type, int playerLevel, long now, double amount) {
        meters.put(type, new Meter(current(type, playerLevel, now) + amount, now));
    }

    public boolean isExhausted(NodeType type, int playerLevel, long now) {
        return current(type, playerLevel, now) > type.fatigueCapacity(playerLevel) - 1;
    }

    // Seconds until the player can gather one more of this resource
    public int secondsUntilReady(NodeType type, int playerLevel, long now) {
        double excess = current(type, playerLevel, now) - (type.fatigueCapacity(playerLevel) - 1);
        if (excess <= 0) return 0;
        return (int) Math.ceil(excess / drainPerTick(type, playerLevel) / 20.0);
    }

    private static double drainPerTick(NodeType type, int playerLevel) {
        return type.fatiguePerHour(playerLevel) / 72000.0; // 72,000 ticks per hour
    }
}
