package com.toddlandis.skillcraft.network;

import com.toddlandis.skillcraft.SkillCraft;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

// Client -> server: the player finished (or failed) the attunement minigame
public record AttunementResultPayload(BlockPos pos, boolean success) implements CustomPacketPayload {
    public static final Type<AttunementResultPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(SkillCraft.MODID, "attunement_result"));

    public static final StreamCodec<ByteBuf, AttunementResultPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, AttunementResultPayload::pos,
            ByteBufCodecs.BOOL, AttunementResultPayload::success,
            AttunementResultPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
