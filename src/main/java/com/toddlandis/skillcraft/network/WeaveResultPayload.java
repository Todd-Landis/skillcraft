package com.toddlandis.skillcraft.network;

import java.util.List;

import com.toddlandis.skillcraft.SkillCraft;
import com.toddlandis.skillcraft.altar.minigame.Minigames;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

// Client -> server: the inputs from a careful weave. The server replays them to score the weave.
public record WeaveResultPayload(BlockPos pos, List<Minigames.Input> inputs) implements CustomPacketPayload {
    public static final Type<WeaveResultPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(SkillCraft.MODID, "weave_result"));

    public static final StreamCodec<ByteBuf, WeaveResultPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, WeaveResultPayload::pos,
            Minigames.Input.LIST_STREAM_CODEC, WeaveResultPayload::inputs,
            WeaveResultPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
