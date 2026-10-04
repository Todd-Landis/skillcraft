package com.toddlandis.skillcraft.network;

import com.toddlandis.skillcraft.SkillCraft;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

// Server -> client: a level-up or a Codex unlocked an ability; the client shows a toast
public record AbilityUnlockedPayload(Identifier ability) implements CustomPacketPayload {
    public static final Type<AbilityUnlockedPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(SkillCraft.MODID, "ability_unlocked"));
    public static final StreamCodec<ByteBuf, AbilityUnlockedPayload> STREAM_CODEC = Identifier.STREAM_CODEC.map(AbilityUnlockedPayload::new, AbilityUnlockedPayload::ability);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
