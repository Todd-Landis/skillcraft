package com.toddlandis.skillcraft.network;

import com.toddlandis.skillcraft.SkillCraft;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

// Server -> client: an ability couldn't be used; the client plays the "no" sound (the reason is
// already on the overlay)
public record AbilityDeniedPayload() implements CustomPacketPayload {
    public static final Type<AbilityDeniedPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(SkillCraft.MODID, "ability_denied"));
    public static final StreamCodec<ByteBuf, AbilityDeniedPayload> STREAM_CODEC = StreamCodec.unit(new AbilityDeniedPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
