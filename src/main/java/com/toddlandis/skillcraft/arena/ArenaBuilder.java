package com.toddlandis.skillcraft.arena;

import com.toddlandis.skillcraft.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

// Builds (and rebuilds) the Sunken Forge arena on a plot. Everything is placed from code so the
// layout is easy to tweak, and every new fight starts from a clean copy: whatever a previous
// fight left behind on the plot is cleared first.
//
// Layout (radius 15): deepslate floor with a smooth basalt dais in the middle, four basalt
// pillars for cover, a blackstone rim wall with shroomlights, an invisible barrier dome so no
// one can leave the plot, and the Exit Portal at the south edge where players arrive.
public final class ArenaBuilder {
    public static final int RADIUS = 15;
    public static final int HEIGHT = 12;
    private static final int CLEAR = RADIUS + 5;
    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    private ArenaBuilder() {}

    // center = the block players stand in at the arena's middle (the floor is one below)
    public static void build(ServerLevel level, BlockPos center) {
        RandomSource random = RandomSource.create(center.asLong());
        int floorY = center.getY() - 1;
        for (int cx = (center.getX() - CLEAR) >> 4; cx <= (center.getX() + CLEAR) >> 4; cx++) {
            for (int cz = (center.getZ() - CLEAR) >> 4; cz <= (center.getZ() + CLEAR) >> 4; cz++) level.getChunk(cx, cz);
        }

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -CLEAR; dx <= CLEAR; dx++) {
            for (int dz = -CLEAR; dz <= CLEAR; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                for (int y = floorY - 2; y <= floorY + HEIGHT + 1; y++) {
                    pos.set(center.getX() + dx, y, center.getZ() + dz);
                    set(level, pos, blockAt(dx, dz, d, y - floorY, random));
                }
            }
        }

        // Pillars (2x2, 5 high) at radius 9 on the diagonals, with a lantern on top
        for (int[] corner : new int[][]{{6, 6}, {-7, 6}, {6, -7}, {-7, -7}}) {
            for (int px = 0; px < 2; px++) for (int pz = 0; pz < 2; pz++) {
                for (int y = 1; y <= 5; y++) set(level, pos.set(center.getX() + corner[0] + px, floorY + y, center.getZ() + corner[1] + pz), Blocks.POLISHED_BASALT.defaultBlockState());
            }
            set(level, pos.set(center.getX() + corner[0], floorY + 6, center.getZ() + corner[1]), Blocks.LANTERN.defaultBlockState());
        }

        // Exit Portal where players arrive
        set(level, pos.set(center.getX(), floorY + 1, center.getZ() + RADIUS - 1), ModBlocks.EXIT_PORTAL.get().defaultBlockState());
    }

    private static BlockState blockAt(int dx, int dz, double d, int y, RandomSource random) {
        double wallInner = RADIUS + 0.5, wallOuter = RADIUS + 2.5, domeOuter = RADIUS + 3.5;
        if (d >= domeOuter) return Blocks.AIR.defaultBlockState();
        if (y == HEIGHT + 1) return Blocks.BARRIER.defaultBlockState();          // ceiling
        if (d >= wallOuter) return y >= -1 ? Blocks.BARRIER.defaultBlockState() : Blocks.AIR.defaultBlockState(); // dome walls
        if (y < -1) return Blocks.AIR.defaultBlockState();
        if (y == -1) return Blocks.DEEPSLATE_BRICKS.defaultBlockState();          // foundation
        if (d >= wallInner) {
            if (y > 6) return Blocks.AIR.defaultBlockState();
            if (y == 6) return Blocks.POLISHED_BLACKSTONE_BRICK_SLAB.defaultBlockState();
            boolean light = y == 3 && Math.floorMod(Math.round(Math.toDegrees(Math.atan2(dz, dx))), 30) < 4;
            return light ? Blocks.SHROOMLIGHT.defaultBlockState() : Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
        }
        if (y > 0) return Blocks.AIR.defaultBlockState();
        // Floor (y == 0)
        if (d < 2.5) return Blocks.SMOOTH_BASALT.defaultBlockState();
        if (d < 3.5) return Blocks.POLISHED_BLACKSTONE.defaultBlockState();
        if (Math.abs(d - 7) < 0.5 && Math.floorMod(Math.round(Math.toDegrees(Math.atan2(dz, dx))), 45) < 6) return Blocks.SHROOMLIGHT.defaultBlockState();
        if (Math.abs(d - 12.5) < 0.6) return Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
        if (random.nextInt(9) == 0) return Blocks.CRACKED_DEEPSLATE_TILES.defaultBlockState();
        return ((dx + dz) & 1) == 0 ? Blocks.POLISHED_DEEPSLATE.defaultBlockState() : Blocks.DEEPSLATE_TILES.defaultBlockState();
    }

    private static void set(ServerLevel level, BlockPos pos, BlockState state) {
        if (level.getBlockState(pos) != state) level.setBlock(pos, state, FLAGS);
    }
}
