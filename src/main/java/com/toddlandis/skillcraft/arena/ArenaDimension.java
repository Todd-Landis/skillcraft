package com.toddlandis.skillcraft.arena;

import com.toddlandis.skillcraft.SkillCraft;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

// The boss arena dimension, defined in data/skillcraft/dimension/arena.json and
// dimension_type/arena.json: a void flat world with fixed time, no skylight, weather or spawning.
public final class ArenaDimension {
    public static final ResourceKey<Level> LEVEL = ResourceKey.create(Registries.DIMENSION, Identifier.fromNamespaceAndPath(SkillCraft.MODID, "arena"));

    // GameTests only: the GameTest server doesn't create datapack dimensions, so tests run the
    // arena on a stand-in level. Never set in normal play.
    private static ServerLevel standIn;

    private ArenaDimension() {}

    public static ServerLevel level(MinecraftServer server) {
        ServerLevel level = server.getLevel(LEVEL);
        return level != null ? level : standIn;
    }

    public static boolean is(LevelAccessor level) {
        return level instanceof Level l && (l.dimension() == LEVEL || (standIn != null && l == standIn));
    }

    public static boolean isKey(MinecraftServer server, ResourceKey<Level> key) {
        return key == LEVEL || (standIn != null && standIn.dimension() == key);
    }

    public static void useStandInForTests(ServerLevel level) {
        standIn = level;
    }
}
