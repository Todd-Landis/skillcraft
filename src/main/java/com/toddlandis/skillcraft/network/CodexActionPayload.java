package com.toddlandis.skillcraft.network;

import java.util.UUID;

import com.toddlandis.skillcraft.SkillCraft;
import com.toddlandis.skillcraft.bonding.BondCodex;
import com.toddlandis.skillcraft.bonding.BondService;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

// Client -> server: a button on the Bond Codex screen. The server checks everything again
// (state, level, shrine, tribute) in BondService; the client only asks.
public record CodexActionPayload(Action action, UUID bond) implements CustomPacketPayload {
    public enum Action { SUMMON, STORE, RELEASE, REKINDLE }

    public static final Type<CodexActionPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(SkillCraft.MODID, "codex_action"));

    public static final StreamCodec<ByteBuf, CodexActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT.map(i -> Action.values()[i], Action::ordinal), CodexActionPayload::action,
            UUIDUtil.STREAM_CODEC, CodexActionPayload::bond,
            CodexActionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CodexActionPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        switch (payload.action()) {
            case SUMMON -> BondService.summon(player, payload.bond());
            case STORE -> {
                BondCodex.Bond bond = BondService.codex(player).find(payload.bond());
                if (bond != null) BondService.store(player, bond);
            }
            case RELEASE -> BondService.release(player, payload.bond());
            case REKINDLE -> BondService.rekindle(player, payload.bond());
        }
    }
}
