package com.toddlandis.skillcraft.network;

import com.toddlandis.skillcraft.SkillCraft;
import com.toddlandis.skillcraft.leycraft.LeyElement;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

// Server -> client: open the careful-weave minigame (Steady the Current) at a Ley Altar. The seed
// and tier make the game identical on both sides so the server can verify it (see LeyWeaving).
public record OpenWeavePayload(BlockPos pos, LeyElement element, int tier, long seed) implements CustomPacketPayload {
    public static final Type<OpenWeavePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(SkillCraft.MODID, "open_weave"));

    public static final StreamCodec<ByteBuf, OpenWeavePayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, OpenWeavePayload::pos,
            ByteBufCodecs.VAR_INT.map(i -> LeyElement.values()[i], LeyElement::ordinal), OpenWeavePayload::element,
            ByteBufCodecs.VAR_INT, OpenWeavePayload::tier,
            ByteBufCodecs.VAR_LONG, OpenWeavePayload::seed,
            OpenWeavePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
