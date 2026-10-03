package com.toddlandis.skillcraft.network;

import java.util.List;

import com.toddlandis.skillcraft.SkillCraft;
import com.toddlandis.skillcraft.altar.minigame.Minigames;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

// Client -> server: every button press/release the player made during the attunement minigame.
// The client does not say whether it won; the server replays the inputs to find out.
public record AttunementResultPayload(BlockPos pos, List<Minigames.Input> inputs) implements CustomPacketPayload {
    public static final Type<AttunementResultPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(SkillCraft.MODID, "attunement_result"));

    public static final StreamCodec<ByteBuf, AttunementResultPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, AttunementResultPayload::pos,
            Minigames.Input.LIST_STREAM_CODEC, AttunementResultPayload::inputs,
            AttunementResultPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
