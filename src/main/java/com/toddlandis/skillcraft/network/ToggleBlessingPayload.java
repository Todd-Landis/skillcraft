package com.toddlandis.skillcraft.network;

import com.toddlandis.skillcraft.SkillCraft;
import com.toddlandis.skillcraft.ability.AbilityService;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

// Client -> server: a blessing was right-clicked in the Ability Book's Prayers tab. The server
// accepts only Prayer toggles here (AbilityService.useFromBook) and runs the usual checks.
public record ToggleBlessingPayload(Identifier ability) implements CustomPacketPayload {
    public static final Type<ToggleBlessingPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(SkillCraft.MODID, "toggle_blessing"));
    public static final StreamCodec<ByteBuf, ToggleBlessingPayload> STREAM_CODEC = Identifier.STREAM_CODEC.map(ToggleBlessingPayload::new, ToggleBlessingPayload::ability);

    public static void handle(ToggleBlessingPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) AbilityService.useFromBook(player, payload.ability());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
