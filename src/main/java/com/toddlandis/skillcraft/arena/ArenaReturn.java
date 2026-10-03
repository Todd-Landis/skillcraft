package com.toddlandis.skillcraft.arena;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

// Where a player entered a boss arena from. Stored on the player (attachment arena_return),
// so even after a crash or logout mid-fight they are sent back here on their next login.
public record ArenaReturn(Optional<ResourceKey<Level>> dimension, Vec3 pos, float yRot, float xRot) {
    public static final ArenaReturn NONE = new ArenaReturn(Optional.empty(), Vec3.ZERO, 0, 0);

    public static final MapCodec<ArenaReturn> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ResourceKey.codec(Registries.DIMENSION).optionalFieldOf("dimension").forGetter(ArenaReturn::dimension),
            Vec3.CODEC.optionalFieldOf("pos", Vec3.ZERO).forGetter(ArenaReturn::pos),
            Codec.FLOAT.optionalFieldOf("y_rot", 0f).forGetter(ArenaReturn::yRot),
            Codec.FLOAT.optionalFieldOf("x_rot", 0f).forGetter(ArenaReturn::xRot)
    ).apply(instance, ArenaReturn::new));

    public boolean isSet() {
        return dimension.isPresent();
    }
}
