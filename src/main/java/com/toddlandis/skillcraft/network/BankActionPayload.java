package com.toddlandis.skillcraft.network;

import com.toddlandis.skillcraft.SkillCraft;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

// Client -> server: one bank action (see BankActions.Action).
//   index  - bank entry index (withdraw/move/tab) or menu slot index (deposit)
//   amount - how many, or BankActions.ALL / ALL_BUT_ONE
//   target - destination index (swap/insert), tab number (set tab), or tab for new deposits
public record BankActionPayload(int containerId, int action, int index, int amount, int target) implements CustomPacketPayload {
    public static final Type<BankActionPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(SkillCraft.MODID, "bank_action"));

    public static final StreamCodec<ByteBuf, BankActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, BankActionPayload::containerId,
            ByteBufCodecs.VAR_INT, BankActionPayload::action,
            ByteBufCodecs.VAR_INT, BankActionPayload::index,
            ByteBufCodecs.VAR_INT, BankActionPayload::amount,
            ByteBufCodecs.VAR_INT, BankActionPayload::target,
            BankActionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
