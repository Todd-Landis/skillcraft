package com.toddlandis.skillcraft.node;

import java.util.List;

import com.mojang.serialization.MapCodec;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;

// Chunk attachment: positions of heart-dropping blocks (logs, ores) that a player placed. Breaking
// one of these never drops an Elemental Heart, so placing and re-mining a silk-touched ore (or a
// log) can't farm hearts. Entries are removed when the block is broken.
public final class PlacedResources {
    public static final MapCodec<PlacedResources> CODEC = com.mojang.serialization.Codec.LONG.listOf().optionalFieldOf("placed", List.of())
            .xmap(PlacedResources::new, placed -> List.copyOf(placed.positions));

    private final LongOpenHashSet positions = new LongOpenHashSet();

    public PlacedResources() {}

    private PlacedResources(List<Long> saved) {
        positions.addAll(saved);
    }

    public void add(BlockPos pos) {
        positions.add(pos.asLong());
    }

    // Removes the position; true if it was a placed block
    public boolean remove(BlockPos pos) {
        return positions.remove(pos.asLong());
    }

    public boolean contains(BlockPos pos) {
        return positions.contains(pos.asLong());
    }
}
