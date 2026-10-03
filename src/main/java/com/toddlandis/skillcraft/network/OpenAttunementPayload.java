package com.toddlandis.skillcraft.network;

import com.toddlandis.skillcraft.SkillCraft;
import com.toddlandis.skillcraft.node.NodeType;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

// Server -> client: open the attunement minigame for a full altar
public record OpenAttunementPayload(BlockPos pos, NodeType node) implements CustomPacketPayload {
    public static final Type<OpenAttunementPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(SkillCraft.MODID, "open_attunement"));

    public static final StreamCodec<ByteBuf, OpenAttunementPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, OpenAttunementPayload::pos,
            ByteBufCodecs.VAR_INT.map(i -> NodeType.values()[i], NodeType::ordinal), OpenAttunementPayload::node,
            OpenAttunementPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
