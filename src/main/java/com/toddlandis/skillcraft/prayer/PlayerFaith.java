package com.toddlandis.skillcraft.prayer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;

// A player's Faith, the resource blessings drain (attachment "faith"). Saved, and synced to its
// owner with the maximum the server worked out, so the HUD never depends on the client's config.
// A new or respawned player starts full (points = -1 means "full").
public final class PlayerFaith {
    public static final MapCodec<PlayerFaith> CODEC = Codec.FLOAT.optionalFieldOf("points", -1f)
            .xmap(PlayerFaith::new, faith -> faith.points);

    public static final StreamCodec<ByteBuf, PlayerFaith> STREAM_CODEC = StreamCodec.of(
            (buf, faith) -> {
                buf.writeFloat(faith.points);
                buf.writeFloat(faith.max);
            },
            buf -> {
                PlayerFaith faith = new PlayerFaith(buf.readFloat());
                faith.max = buf.readFloat();
                return faith;
            });

    private float points;
    private float max = 10;

    public PlayerFaith() {
        this(-1f);
    }

    private PlayerFaith(float points) {
        this.points = points;
    }

    // Current Faith, given the maximum
    public float points() {
        return points < 0 ? max : Math.min(points, max);
    }

    public float max() {
        return max;
    }

    void setMax(float max) {
        this.max = max;
    }

    void setPoints(float value) {
        points = Math.clamp(value, 0, max);
    }

    public boolean isFull() {
        return points() >= max - 1.0E-4f;
    }
}
