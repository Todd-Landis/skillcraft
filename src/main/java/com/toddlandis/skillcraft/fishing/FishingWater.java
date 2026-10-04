package com.toddlandis.skillcraft.fishing;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

// Where a fishing spot may sit. A spot needs real open water, not a bucket of water in a hole:
//   - the tile is a still water source with open air above it
//   - the water is at least DEPTH blocks deep under the spot
//   - the tile is part of a pond of at least NodeType.minWaterArea() connected open-water tiles
//     (12 for tier I, +6 per tier), counted across the surface out to SCAN_RADIUS blocks
//   - at least MIN_WET_NEIGHBOURS of the 8 tiles around it are open water, so a spot can't sit
//     in a one-block-wide channel cut through a big lake
public final class FishingWater {
    public static final int DEPTH = 2;
    public static final int SCAN_RADIUS = 8;
    public static final int MIN_WET_NEIGHBOURS = 5;
    // A tile within this many blocks of dry ground can be reached from the shore
    public static final int SHORE_REACH = 2;

    private FishingWater() {}

    // A still water source block with nothing on top of it
    public static boolean isOpenWater(Level level, BlockPos pos) {
        return level.getBlockState(pos).is(Blocks.WATER) && level.getFluidState(pos).isSource()
                && level.getBlockState(pos.above()).isAir();
    }

    // The connected open-water surface tiles around pos (same height), up to SCAN_RADIUS away
    public static Set<BlockPos> surface(Level level, BlockPos pos) {
        Set<BlockPos> found = new HashSet<>();
        if (!isOpenWater(level, pos)) return found;
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(pos.immutable());
        found.add(pos.immutable());
        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos next = current.relative(direction);
                if (Math.abs(next.getX() - pos.getX()) > SCAN_RADIUS || Math.abs(next.getZ() - pos.getZ()) > SCAN_RADIUS) continue;
                if (found.contains(next) || !isOpenWater(level, next)) continue;
                found.add(next);
                queue.add(next);
            }
        }
        return found;
    }

    // Why a spot can't go here (a lang key), or null if it can
    public static String problem(Level level, BlockPos pos, int minArea) {
        if (!isOpenWater(level, pos)) return "message.skillcraft.shoal_not_water";
        for (int d = 1; d < DEPTH; d++) {
            if (!level.getBlockState(pos.below(d)).is(Blocks.WATER)) return "message.skillcraft.shoal_too_shallow";
        }
        if (wetNeighbours(level, pos) < MIN_WET_NEIGHBOURS) return "message.skillcraft.shoal_too_narrow";
        if (minArea > 0 && surface(level, pos).size() < minArea) return "message.skillcraft.shoal_too_small";
        return null;
    }

    public static boolean isValid(Level level, BlockPos pos, int minArea) {
        return problem(level, pos, minArea) == null;
    }

    private static int wetNeighbours(Level level, BlockPos pos) {
        int wet = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if ((dx != 0 || dz != 0) && isOpenWater(level, pos.offset(dx, 0, dz))) wet++;
            }
        }
        return wet;
    }

    // True if dry ground is within SHORE_REACH blocks, so a player on the bank can fish it
    public static boolean nearShore(Level level, BlockPos pos) {
        for (int dx = -SHORE_REACH - 1; dx <= SHORE_REACH + 1; dx++) {
            for (int dz = -SHORE_REACH - 1; dz <= SHORE_REACH + 1; dz++) {
                BlockPos ground = pos.offset(dx, 0, dz);
                if (!level.getBlockState(ground).is(Blocks.WATER) && level.getBlockState(ground).isFaceSturdy(level, ground, Direction.UP)
                        && level.getBlockState(ground.above()).isAir()) {
                    return true;
                }
            }
        }
        return false;
    }

    // Tiles a shoal could resurface on: valid, reachable from the shore, and not `except`
    public static List<BlockPos> resurfaceCandidates(Level level, BlockPos from, int minArea, Set<BlockPos> except) {
        List<BlockPos> candidates = new ArrayList<>();
        Set<BlockPos> pond = surface(level, from);
        if (pond.size() < minArea) return candidates;
        // Every tile here is in the same pond, so only the local checks are repeated per tile
        for (BlockPos tile : pond) {
            if (except.contains(tile)) continue;
            if (problem(level, tile, 0) == null && nearShore(level, tile)) candidates.add(tile);
        }
        return candidates;
    }
}
