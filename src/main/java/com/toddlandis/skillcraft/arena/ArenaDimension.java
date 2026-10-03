package com.toddlandis.skillcraft.arena;

import com.toddlandis.skillcraft.SkillCraft;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

// The boss arena dimension, defined in data/skillcraft/dimension/arena.json and
// dimension_type/arena.json: a void flat world with fixed time, no skylight, weather or spawning.
public final class ArenaDimension {
    public static final ResourceKey<Level> LEVEL = ResourceKey.create(Registries.DIMENSION, Identifier.fromNamespaceAndPath(SkillCraft.MODID, "arena"));

    private ArenaDimension() {}

    public static boolean is(LevelAccessor level) {
        return level instanceof Level l && l.dimension() == LEVEL;
    }
}
