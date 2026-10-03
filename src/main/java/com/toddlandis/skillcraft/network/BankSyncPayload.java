package com.toddlandis.skillcraft.network;

import java.util.List;

import com.toddlandis.skillcraft.SkillCraft;
import com.toddlandis.skillcraft.bank.PlayerBank;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

// Server -> client: the player's full bank contents, sent when the bank opens and after every change
public record BankSyncPayload(List<PlayerBank.Entry> entries, int capacity) implements CustomPacketPayload {
    public static final Type<BankSyncPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(SkillCraft.MODID, "bank_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BankSyncPayload> STREAM_CODEC = StreamCodec.composite(
            PlayerBank.Entry.STREAM_CODEC.apply(ByteBufCodecs.list(100_000)), BankSyncPayload::entries,
            ByteBufCodecs.VAR_INT, BankSyncPayload::capacity,
            BankSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
