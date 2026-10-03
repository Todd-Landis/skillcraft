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
// pillars for cover, a blackstone rim wall with shroomlights, and a forge dome overhead:
// an elliptical blackstone/deepslate shell with 8 gilded ribs, glowing ember veins, a
// shroomlight oculus at the apex and chains with lanterns hanging over the floor. An invisible
// barrier ring outside the wall keeps anyone from leaving the plot. The Exit Portal sits at the
// south edge where players arrive.
public final class ArenaBuilder {
    public static final int RADIUS = 15;
    // Dome: an ellipse rising from the top of the rim wall (y = WALL_TOP) to the apex
    private static final int WALL_TOP = 6;
    private static final double DOME_RADIUS = 17.5;  // horizontal semi-axis, reaches the wall's outer edge
    private static final double DOME_RISE = 15.0;    // vertical semi-axis above WALL_TOP -> apex at y = 21
    private static final double DOME_SHELL = 0.085;  // shell thickness as a fraction of the ellipse
    public static final int HEIGHT = WALL_TOP + (int) DOME_RISE;
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

        // Chains hanging from the dome with a lantern at the end, between the pillars
        for (int[] spot : new int[][]{{0, -10}, {10, 0}, {-10, 0}, {0, 10}}) {
            int x = center.getX() + spot[0], z = center.getZ() + spot[1];
            // Come down from above the dome, through the shell, to the first air inside it
            int y = floorY + HEIGHT + 1;
            while (y > floorY && level.getBlockState(pos.set(x, y, z)).isAir()) y--;
            while (y > floorY && !level.getBlockState(pos.set(x, y, z)).isAir()) y--;
            int bottom = floorY + 11;
            for (; y > bottom; y--) set(level, pos.set(x, y, z), Blocks.IRON_CHAIN.defaultBlockState());
            set(level, pos.set(x, bottom, z), Blocks.LANTERN.defaultBlockState().setValue(net.minecraft.world.level.block.LanternBlock.HANGING, true));
        }

        // Exit Portal where players arrive
        set(level, pos.set(center.getX(), floorY + 1, center.getZ() + RADIUS - 1), ModBlocks.EXIT_PORTAL.get().defaultBlockState());
    }

    private static BlockState blockAt(int dx, int dz, double d, int y, RandomSource random) {
        double wallInner = RADIUS + 0.5, wallOuter = RADIUS + 2.5, barrierOuter = RADIUS + 3.5;
        if (d >= barrierOuter) return Blocks.AIR.defaultBlockState();
        if (y < -1) return Blocks.AIR.defaultBlockState();
        if (y >= WALL_TOP) {
            BlockState dome = domeAt(dx, dz, d, y, random);
            if (dome != null) return dome;
            // Outside the dome: barrier ring beside the wall, open air above
            return d >= wallOuter && y <= WALL_TOP ? Blocks.BARRIER.defaultBlockState() : Blocks.AIR.defaultBlockState();
        }
        if (d >= wallOuter) return Blocks.BARRIER.defaultBlockState();            // barrier ring
        if (y == -1) return Blocks.DEEPSLATE_BRICKS.defaultBlockState();          // foundation
        if (d >= wallInner) {
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

    // The dome shell (null = not part of it). e is the position on the ellipse: 1 at its surface.
    private static BlockState domeAt(int dx, int dz, double d, int y, RandomSource random) {
        double e = Math.sqrt(Math.pow(d / DOME_RADIUS, 2) + Math.pow((y - WALL_TOP) / DOME_RISE, 2));
        if (e > 1.0 || e < 1.0 - DOME_SHELL) return null;
        // Oculus at the apex: glowing ring around a magma "ember" core
        if (d < 1.5) return Blocks.MAGMA_BLOCK.defaultBlockState();
        if (d < 3.0) return Blocks.SHROOMLIGHT.defaultBlockState();
        // 8 gilded ribs running from the wall to the oculus
        double angle = Math.toDegrees(Math.atan2(dz, dx));
        double offRib = Math.abs(Math.floorMod(Math.round(angle), 45) - 22.5);
        double ribHalfWidth = Math.toDegrees(0.8 / Math.max(d, 3.0)); // about 1.5 blocks wide at any radius
        if (offRib > 22.5 - ribHalfWidth) return Blocks.GILDED_BLACKSTONE.defaultBlockState();
        // Lower band in polished blackstone bricks, upper shell in deepslate tiles, with ember veins
        if (random.nextInt(16) == 0) return Blocks.MAGMA_BLOCK.defaultBlockState();
        if (y <= WALL_TOP + 2) return Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
        return random.nextInt(5) == 0 ? Blocks.CRACKED_DEEPSLATE_TILES.defaultBlockState() : Blocks.DEEPSLATE_TILES.defaultBlockState();
    }

    private static void set(ServerLevel level, BlockPos pos, BlockState state) {
        if (level.getBlockState(pos) != state) level.setBlock(pos, state, FLAGS);
    }
}
