package com.toddlandis.skillcraft.network;

import java.util.List;

import com.toddlandis.skillcraft.SkillCraft;
import com.toddlandis.skillcraft.ability.AbilityData;
import com.toddlandis.skillcraft.ability.AbilityService;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

// Client -> server: set one of the 3 ability bars (slots as ability ids, "" = empty), optionally
// rename it and/or make it the active bar. Validated by AbilityService.setLoadout.
public record SetLoadoutPayload(int loadout, List<String> slots, String name, boolean activate) implements CustomPacketPayload {
    public static final Type<SetLoadoutPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(SkillCraft.MODID, "set_loadout"));
    public static final StreamCodec<ByteBuf, SetLoadoutPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SetLoadoutPayload::loadout,
            ByteBufCodecs.stringUtf8(200).apply(ByteBufCodecs.list(AbilityData.SLOTS)), SetLoadoutPayload::slots,
            ByteBufCodecs.stringUtf8(AbilityData.NAME_MAX * 4), SetLoadoutPayload::name,
            ByteBufCodecs.BOOL, SetLoadoutPayload::activate,
            SetLoadoutPayload::new);

    public static void handle(SetLoadoutPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        Identifier[] slots = new Identifier[Math.min(AbilityData.SLOTS, payload.slots().size())];
        for (int i = 0; i < slots.length; i++) {
            String text = payload.slots().get(i);
            slots[i] = text.isEmpty() ? null : Identifier.tryParse(text);
        }
        AbilityService.setLoadout(player, payload.loadout(), slots, payload.name(), payload.activate());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
