package com.toddlandis.skillcraft.network;

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

// Client -> server: an ability key was pressed. All the checks happen on the server (AbilityService).
public record UseAbilityPayload(int slot) implements CustomPacketPayload {
    public static final Type<UseAbilityPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(SkillCraft.MODID, "use_ability"));
    public static final StreamCodec<ByteBuf, UseAbilityPayload> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(UseAbilityPayload::new, UseAbilityPayload::slot);

    public static void handle(UseAbilityPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        if (payload.slot() < 0 || payload.slot() >= AbilityData.SLOTS) return;
        AbilityService.useSlot(player, payload.slot());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
