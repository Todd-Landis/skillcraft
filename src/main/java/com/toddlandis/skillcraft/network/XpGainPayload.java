package com.toddlandis.skillcraft.network;

import com.toddlandis.skillcraft.SkillCraft;
import com.toddlandis.skillcraft.skill.Skill;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

// Server -> client: "you gained X XP in a skill" so the client can show an XP drop.
// newLevel is 0 unless this gain caused a level-up.
public record XpGainPayload(Skill skill, float amount, int newLevel) implements CustomPacketPayload {
    public static final Type<XpGainPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(SkillCraft.MODID, "xp_gain"));

    public static final StreamCodec<ByteBuf, XpGainPayload> STREAM_CODEC = StreamCodec.composite(
            Skill.STREAM_CODEC, XpGainPayload::skill,
            ByteBufCodecs.FLOAT, XpGainPayload::amount,
            ByteBufCodecs.VAR_INT, XpGainPayload::newLevel,
            XpGainPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
