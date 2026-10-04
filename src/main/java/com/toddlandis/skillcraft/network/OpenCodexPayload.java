package com.toddlandis.skillcraft.network;

import com.toddlandis.skillcraft.SkillCraft;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

// Server -> client: open the Bond Codex screen (sent when the codex item is used in the air)
public record OpenCodexPayload() implements CustomPacketPayload {
    public static final OpenCodexPayload INSTANCE = new OpenCodexPayload();
    public static final Type<OpenCodexPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(SkillCraft.MODID, "open_codex"));
    public static final StreamCodec<ByteBuf, OpenCodexPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
