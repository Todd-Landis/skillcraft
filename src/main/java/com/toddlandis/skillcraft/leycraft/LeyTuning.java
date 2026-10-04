package com.toddlandis.skillcraft.leycraft;

import java.util.IdentityHashMap;
import java.util.Map;

import com.mojang.datafixers.util.Pair;
import com.toddlandis.skillcraft.Config;
import com.toddlandis.skillcraft.registry.ModDataMaps;
import com.toddlandis.skillcraft.skill.Skill;
import com.toddlandis.skillcraft.skill.SkillManager;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.Vec3;

// What the Ley Tuner finds (docs/LEYCRAFT.md section 7): the nearest strong source of its element
// in the loaded world (a block in ley_resonance.json worth at least leyTunerMinPoints: lava,
// packed ice, lightning rods, budding amethyst...), or failing that the nearest biome of that
// element. It answers with a direction, a distance, a trail of the element's particles and a
// chime that rises in pitch the closer the source is.
public final class LeyTuning {
    private LeyTuning() {}

    // Block search radius: grows with Leycraft level
    public static int radius(int leycraftLevel) {
        return Config.LEY_TUNER_RADIUS.get() + leycraftLevel / 2;
    }

    // The nearest strong source block of this element within `radius` (loaded chunks only, so a
    // search never generates or loads terrain), or null
    public static BlockPos nearestSource(ServerLevel level, BlockPos origin, LeyElement element, int radius) {
        double minPoints = Config.LEY_TUNER_MIN_POINTS.get();
        Map<BlockState, Boolean> cache = new IdentityHashMap<>();
        java.util.function.Predicate<BlockState> isSource = state -> cache.computeIfAbsent(state, s -> {
            LeyData.Resonance data = s.typeHolder().getData(ModDataMaps.LEY_RESONANCE);
            return data != null && data.element() == element && data.points() >= minPoints;
        });

        long bestDist = (long) radius * radius + 1;
        BlockPos best = null;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int cx = (origin.getX() - radius) >> 4; cx <= (origin.getX() + radius) >> 4; cx++) {
            for (int cz = (origin.getZ() - radius) >> 4; cz <= (origin.getZ() + radius) >> 4; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) continue;
                LevelChunkSection[] sections = chunk.getSections();
                for (int i = 0; i < sections.length; i++) {
                    LevelChunkSection section = sections[i];
                    int baseY = SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(i));
                    if (baseY > origin.getY() + radius || baseY + 15 < origin.getY() - radius) continue;
                    if (section.hasOnlyAir() || !section.getStates().maybeHas(isSource)) continue;
                    for (int y = 0; y < 16; y++) {
                        for (int z = 0; z < 16; z++) {
                            for (int x = 0; x < 16; x++) {
                                if (!isSource.test(section.getBlockState(x, y, z))) continue;
                                pos.set((cx << 4) + x, baseY + y, (cz << 4) + z);
                                long d = (long) pos.distSqr(origin);
                                if (d < bestDist) {
                                    bestDist = d;
                                    best = pos.immutable();
                                }
                            }
                        }
                    }
                }
            }
        }
        return best;
    }

    // The nearest biome of this element (like /locate biome), or null
    public static BlockPos nearestBiome(ServerLevel level, BlockPos origin, LeyElement element) {
        Pair<BlockPos, Holder<Biome>> found = level.findClosestBiome3d(biome -> biome.is(element.biomes()), origin,
                Config.LEY_TUNER_BIOME_RADIUS.get(), 32, 64);
        return found == null ? null : found.getFirst();
    }

    // Rings the tuner: finds a source, then tells and shows the player where it is
    public static void ring(ServerPlayer player, LeyElement element) {
        ServerLevel level = player.level();
        BlockPos origin = player.blockPosition();
        BlockPos source = nearestSource(level, origin, element, radius(SkillManager.level(player, Skill.LEYCRAFT)));
        Component message;
        BlockPos target;
        if (source != null) {
            target = source;
            message = Component.translatable("message.skillcraft.tuner_source", element.displayName(), level.getBlockState(source).getBlock().getName(),
                    distance(origin, source), direction(origin, source), height(origin, source));
        } else {
            target = nearestBiome(level, origin, element);
            if (target == null) {
                message = Component.translatable("message.skillcraft.tuner_nothing", element.displayName());
            } else if (target.distManhattan(new BlockPos(origin.getX(), target.getY(), origin.getZ())) < 48) {
                message = Component.translatable("message.skillcraft.tuner_biome_here", element.displayName());
                target = null;
            } else {
                message = Component.translatable("message.skillcraft.tuner_biome", element.displayName(), distance(origin, target), direction(origin, target));
            }
        }
        player.sendOverlayMessage(message.copy().withStyle(ChatFormatting.LIGHT_PURPLE));

        if (target == null) {
            level.playSound(null, origin, SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.PLAYERS, 0.8f, 0.6f);
            return;
        }
        // A trail of the element's particles leading off toward the source (the first 8 blocks)
        Vec3 eye = player.getEyePosition();
        Vec3 dir = Vec3.atCenterOf(target).subtract(eye).normalize();
        for (int i = 1; i <= 16; i++) {
            Vec3 p = eye.add(dir.scale(i * 0.5)).add(0, -0.4, 0);
            level.sendParticles(element.particle(), p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0);
        }
        // Closer = higher chime
        double dist = Math.sqrt(target.distSqr(origin));
        float pitch = (float) Math.clamp(2.0 - dist / 64.0, 0.5, 2.0);
        level.playSound(null, origin, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, pitch);
    }

    private static int distance(BlockPos from, BlockPos to) {
        return (int) Math.round(Math.sqrt(Math.pow(to.getX() - from.getX(), 2) + Math.pow(to.getZ() - from.getZ(), 2)));
    }

    // "north", "south-east"...: north is -Z, east is +X
    public static Component direction(BlockPos from, BlockPos to) {
        double bearing = Math.toDegrees(Math.atan2(to.getX() - from.getX(), from.getZ() - to.getZ()));
        int index = Math.floorMod((int) Math.round(bearing / 45.0), 8);
        String[] names = {"n", "ne", "e", "se", "s", "sw", "w", "nw"};
        return Component.translatable("direction.skillcraft." + names[index]);
    }

    // "", " (12 below)", " (6 above)"
    private static Component height(BlockPos from, BlockPos to) {
        int dy = to.getY() - from.getY();
        if (Math.abs(dy) < 3) return Component.empty();
        return Component.translatable(dy > 0 ? "message.skillcraft.tuner_above" : "message.skillcraft.tuner_below", Math.abs(dy));
    }
}
