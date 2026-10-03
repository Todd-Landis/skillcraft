package com.toddlandis.skillcraft.gathering;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;

// Per-chunk set of player-placed resource blocks. Breaking one of these gives no XP.
public class PlacedBlocks {
    public static final MapCodec<PlacedBlocks> CODEC = Codec.LONG.listOf()
            .xmap(PlacedBlocks::new, placed -> List.copyOf(placed.positions))
            .fieldOf("positions");

    private final Set<Long> positions = new HashSet<>();

    public PlacedBlocks() {}

    private PlacedBlocks(List<Long> saved) {
        positions.addAll(saved);
    }

    public void add(BlockPos pos) {
        positions.add(pos.asLong());
    }

    // Returns true if the position was player-placed (and forgets it)
    public boolean remove(BlockPos pos) {
        return positions.remove(pos.asLong());
    }
}
