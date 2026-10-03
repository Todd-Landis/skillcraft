package com.toddlandis.skillcraft.arena;

import com.toddlandis.skillcraft.bank.BankChestBlock;
import com.toddlandis.skillcraft.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.BlastFurnaceBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.StairBlock;
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
    // Gate, corridor and lobby south of the arena (distances from the center along +Z)
    private static final int GATE_END = RADIUS + 4;
    public static final int LOBBY_START = RADIUS + 9;
    public static final int LOBBY_END = LOBBY_START + 14;
    public static final int LOBBY_SPAWN = LOBBY_END - 4;
    private static final int LOBBY_HALF_WIDTH = 8;
    private static final int LOBBY_HEIGHT = 8;
    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    private ArenaBuilder() {}

    // center = the block players stand in at the arena's middle (the floor is one below)
    public static void build(ServerLevel level, BlockPos center) {
        RandomSource random = RandomSource.create(center.asLong());
        int floorY = center.getY() - 1;
        for (int cx = (center.getX() - CLEAR) >> 4; cx <= (center.getX() + CLEAR) >> 4; cx++) {
            for (int cz = (center.getZ() - CLEAR) >> 4; cz <= (center.getZ() + LOBBY_END + 1) >> 4; cz++) level.getChunk(cx, cz);
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

        for (int[] corner : new int[][]{{7, 7}, {-7, 7}, {7, -7}, {-7, -7}}) grandPillar(level, center, corner[0], corner[1]);
        greatForge(level, center);

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

        gateCorridorAndLobby(level, center);
    }

    // --- pieces (dx/dz from the arena center, y = blocks above the floor) ---

    private static void put(ServerLevel level, BlockPos center, int dx, int y, int dz, BlockState state) {
        set(level, center.offset(dx, y - 1, dz), state);
    }

    private static boolean isAir(ServerLevel level, BlockPos center, int dx, int y, int dz) {
        return level.getBlockState(center.offset(dx, y - 1, dz)).isAir();
    }

    // A 3x3 basalt column on a 5x5 plinth, banded with chiseled and gilded blackstone and glowing
    // ember inlays, rising to meet the dome under a wide capital with hanging lanterns. The four
    // columns sit on the diagonals, right under four of the dome's ribs.
    private static void grandPillar(ServerLevel level, BlockPos center, int cx, int cz) {
        BlockState bricks = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
        for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) put(level, center, cx + x, 1, cz + z, bricks);
        int top = 2;
        while (top < HEIGHT && isAir(level, center, cx, top, cz)) top++;
        for (int y = 2; y < top; y++) {
            for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
                boolean faceMiddle = (x == 0) != (z == 0);
                BlockState state;
                if (y == 2 || y == 6 || y == 11) state = Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState();
                else if (y == top - 4) state = Blocks.GILDED_BLACKSTONE.defaultBlockState();
                else if (faceMiddle && (y == 8 || y == 9)) state = Blocks.MAGMA_BLOCK.defaultBlockState();
                else state = Blocks.POLISHED_BASALT.defaultBlockState();
                put(level, center, cx + x, y, cz + z, state);
            }
        }
        // Capital: a wide brick ring just under the dome, lanterns hanging from its corners
        int capital = top - 1;
        for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) {
            if (Math.abs(x) == 2 || Math.abs(z) == 2) put(level, center, cx + x, capital, cz + z, bricks);
        }
        for (int x : new int[]{-2, 2}) for (int z : new int[]{-2, 2}) {
            put(level, center, cx + x, capital - 1, cz + z, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        }
    }

    // The Great Forge on the north side, behind where Slagheart wakes: a brick hearth with a lava
    // firebox behind iron bars, a chimney up into the dome, lit blast furnaces either side, a great
    // anvil and a quench trough.
    private static void greatForge(ServerLevel level, BlockPos center) {
        BlockState bricks = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
        // Hearth body
        for (int x = -4; x <= 4; x++) for (int z = -17; z <= -14; z++) for (int y = 1; y <= 6; y++) put(level, center, x, y, z, bricks);
        // Firebox: lava behind bars, framed by a gilded lintel
        for (int x = -2; x <= 2; x++) {
            put(level, center, x, 1, -15, Blocks.LAVA.defaultBlockState());
            for (int y = 2; y <= 3; y++) put(level, center, x, y, -15, Blocks.AIR.defaultBlockState());
            for (int y = 1; y <= 3; y++) put(level, center, x, y, -14, Blocks.IRON_BARS.defaultBlockState());
        }
        for (int x = -3; x <= 3; x++) put(level, center, x, 4, -14, Blocks.GILDED_BLACKSTONE.defaultBlockState());
        for (int x : new int[]{-4, 4}) put(level, center, x, 6, -14, Blocks.MAGMA_BLOCK.defaultBlockState());
        // Chimney up into the dome
        for (int y = 7; y < HEIGHT && (isAir(level, center, 0, y, -15) || y < 9); y++) {
            for (int x = -1; x <= 1; x++) for (int z = -16; z <= -15; z++) {
                boolean ember = x == 0 && z == -15 && y % 3 == 0;
                put(level, center, x, y, z, ember ? Blocks.MAGMA_BLOCK.defaultBlockState() : bricks);
            }
        }
        // Lit blast furnaces either side of the hearth
        BlockState furnace = Blocks.BLAST_FURNACE.defaultBlockState().setValue(BlastFurnaceBlock.FACING, Direction.SOUTH).setValue(BlastFurnaceBlock.LIT, true);
        put(level, center, -5, 1, -14, furnace);
        put(level, center, 5, 1, -14, furnace);
        // Great anvil on a pedestal, and a quench trough
        for (int x = -7; x <= -6; x++) {
            put(level, center, x, 1, -11, Blocks.POLISHED_BLACKSTONE.defaultBlockState());
            put(level, center, x, 2, -11, Blocks.ANVIL.defaultBlockState().setValue(AnvilBlock.FACING, Direction.EAST));
        }
        for (int x = 6; x <= 7; x++) put(level, center, x, 1, -11, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
    }

    // A gateway through the south wall, a corridor, and the lobby where players arrive and gather.
    // The lobby has the Exit Portal home, a Bank Chest, benches and lanterns.
    private static void gateCorridorAndLobby(ServerLevel level, BlockPos center) {
        BlockState bricks = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
        BlockState floor = Blocks.POLISHED_BLACKSTONE.defaultBlockState();
        BlockState ceiling = Blocks.DEEPSLATE_TILES.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();

        // Gate: opening through the wall with a gilded frame on the arena side
        for (int x = -1; x <= 1; x++) for (int z = RADIUS; z <= GATE_END; z++) for (int y = 1; y <= 4; y++) put(level, center, x, y, z, air);
        for (int y = 1; y <= 5; y++) for (int x : new int[]{-2, 2}) put(level, center, x, y, RADIUS, Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState());
        for (int x = -1; x <= 1; x++) put(level, center, x, 5, RADIUS, Blocks.GILDED_BLACKSTONE.defaultBlockState());

        // Corridor
        for (int z = RADIUS + 1; z < LOBBY_START; z++) {
            for (int x = -2; x <= 2; x++) {
                put(level, center, x, 0, z, Math.abs(x) == 2 ? bricks : floor);
                put(level, center, x, 5, z, ceiling);
                for (int y = 1; y <= 4; y++) put(level, center, x, y, z, Math.abs(x) == 2 ? bricks : air);
            }
        }
        put(level, center, 0, 4, RADIUS + 5, Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));

        // Lobby shell
        for (int x = -LOBBY_HALF_WIDTH; x <= LOBBY_HALF_WIDTH; x++) {
            for (int z = LOBBY_START; z <= LOBBY_END; z++) {
                boolean edge = Math.abs(x) == LOBBY_HALF_WIDTH || z == LOBBY_START || z == LOBBY_END;
                put(level, center, x, 0, z, (x + z & 1) == 0 ? floor : Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
                put(level, center, x, LOBBY_HEIGHT, z, ceiling);
                for (int y = 1; y < LOBBY_HEIGHT; y++) {
                    BlockState wall = y == 4 && (x + z) % 4 == 0 ? Blocks.SHROOMLIGHT.defaultBlockState() : bricks;
                    put(level, center, x, y, z, edge ? wall : air);
                }
            }
        }
        // Doorway from the corridor
        for (int x = -1; x <= 1; x++) for (int y = 1; y <= 4; y++) put(level, center, x, y, LOBBY_START, air);
        // Red carpet to the gate, benches along the walls, lanterns overhead
        for (int z = LOBBY_START + 1; z < LOBBY_END; z++) put(level, center, 0, 1, z, Blocks.CARPET.red().defaultBlockState());
        for (int z = LOBBY_START + 3; z <= LOBBY_END - 4; z++) {
            put(level, center, -(LOBBY_HALF_WIDTH - 1), 1, z, Blocks.POLISHED_BLACKSTONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST));
            put(level, center, LOBBY_HALF_WIDTH - 1, 1, z, Blocks.POLISHED_BLACKSTONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST));
        }
        for (int[] spot : new int[][]{{-4, LOBBY_START + 4}, {4, LOBBY_START + 4}, {-4, LOBBY_END - 4}, {4, LOBBY_END - 4}, {0, (LOBBY_START + LOBBY_END) / 2}}) {
            put(level, center, spot[0], LOBBY_HEIGHT - 1, spot[1], Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        }
        // Exit Portal home at the back wall, a Bank Chest beside it
        put(level, center, 0, 1, LOBBY_END - 1, ModBlocks.EXIT_PORTAL.get().defaultBlockState());
        put(level, center, -(LOBBY_HALF_WIDTH - 2), 1, LOBBY_END - 2, ModBlocks.BANK_CHEST.get().defaultBlockState().setValue(BankChestBlock.FACING, Direction.EAST));
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
