package com.toddlandis.skillcraft.leycraft;

import java.util.EnumMap;
import java.util.Map;

import com.toddlandis.skillcraft.Config;
import com.toddlandis.skillcraft.registry.ModDataMaps;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;

// What a Ley Altar resonates with (docs/LEYCRAFT.md section 4): the blocks around it
// (data_maps/block/ley_resonance.json), its biome (biome tags) and, for Storm, weather and height.
// The strongest element is the one it weaves; the more resonance, the more motes per shard.
public final class Resonance {
    private Resonance() {}

    // Each element's resonance from 0 to 1, and the strongest (ties go to the earlier element, so Arcane)
    public record Reading(Map<LeyElement, Double> levels, LeyElement dominant) {
        public double of(LeyElement element) { return levels.getOrDefault(element, 0.0); }
        public double strength() { return of(dominant); }
    }

    public static Reading read(Level level, BlockPos altar) {
        Map<LeyElement, Double> points = new EnumMap<>(LeyElement.class);
        int r = Config.LEY_SCAN_RADIUS.get();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    pos.set(altar.getX() + dx, altar.getY() + dy, altar.getZ() + dz);
                    BlockState state = level.getBlockState(pos);
                    if (state.isAir()) continue;
                    LeyData.Resonance source = state.typeHolder().getData(ModDataMaps.LEY_RESONANCE);
                    if (source != null) points.merge(source.element(), source.points(), Double::sum);
                }
            }
        }

        double full = Config.LEY_POINTS_FOR_FULL.get();
        Holder<Biome> biome = level.getBiome(altar);
        boolean openSky = level.canSeeSky(altar.above());
        Map<LeyElement, Double> levels = new EnumMap<>(LeyElement.class);
        LeyElement dominant = LeyElement.ARCANE;
        for (LeyElement element : LeyElement.values()) {
            double value = points.getOrDefault(element, 0.0) / full;
            if (biome.is(element.biomes())) value += Config.LEY_BIOME_BONUS.get();
            if (element == LeyElement.STORM && openSky) {
                if (level.isThundering()) value += Config.LEY_THUNDER_BONUS.get();
                else if (level.isRaining()) value += Config.LEY_RAIN_BONUS.get();
                if (altar.getY() >= Config.LEY_HEIGHT_Y.get()) value += Config.LEY_HEIGHT_BONUS.get();
            }
            value = Math.min(1.0, value);
            levels.put(element, value);
            if (value > levels.get(dominant)) dominant = element;
        }
        return new Reading(levels, dominant);
    }
}
