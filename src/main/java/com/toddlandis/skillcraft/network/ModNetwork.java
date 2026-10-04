package com.toddlandis.skillcraft.network;

import com.toddlandis.skillcraft.SkillCraft;
import com.toddlandis.skillcraft.altar.AltarAttunement;
import com.toddlandis.skillcraft.bank.BankActions;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@EventBusSubscriber(modid = SkillCraft.MODID)
public class ModNetwork {
    // Sends only if the player's connection can take this payload (GameTest mock players and
    // connections without the mod can't, and sending would throw)
    public static void sendTo(net.minecraft.server.level.ServerPlayer player, net.minecraft.network.protocol.common.custom.CustomPacketPayload payload) {
        if (player.connection == null || player instanceof net.neoforged.neoforge.common.util.FakePlayer) return;
        try {
            if (player.connection.hasChannel(payload)) net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, payload);
        } catch (RuntimeException e) {
            // a test connection without channel info: nothing to send to
        }
    }

    @SubscribeEvent
    static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        // Handlers for client-bound packets are registered in client/ClientEvents
        registrar.playToClient(XpGainPayload.TYPE, XpGainPayload.STREAM_CODEC);
        registrar.playToClient(OpenAttunementPayload.TYPE, OpenAttunementPayload.STREAM_CODEC);
        registrar.playToServer(AttunementResultPayload.TYPE, AttunementResultPayload.STREAM_CODEC, AltarAttunement::handleResult);
        registrar.playToServer(BenchActionPayload.TYPE, BenchActionPayload.STREAM_CODEC, BenchActionPayload::handle);
        registrar.playToServer(BankActionPayload.TYPE, BankActionPayload.STREAM_CODEC, BankActions::handle);
        registrar.playToClient(BankSyncPayload.TYPE, BankSyncPayload.STREAM_CODEC);
        registrar.playToServer(CycleAttackStylePayload.TYPE, CycleAttackStylePayload.STREAM_CODEC, CycleAttackStylePayload::handle);
        // Abilities (see ability/AbilityService)
        registrar.playToServer(UseAbilityPayload.TYPE, UseAbilityPayload.STREAM_CODEC, UseAbilityPayload::handle);
        registrar.playToServer(SetLoadoutPayload.TYPE, SetLoadoutPayload.STREAM_CODEC, SetLoadoutPayload::handle);
        registrar.playToClient(AbilityDefinitionsPayload.TYPE, AbilityDefinitionsPayload.STREAM_CODEC);
        registrar.playToClient(AbilityCooldownPayload.TYPE, AbilityCooldownPayload.STREAM_CODEC);
        registrar.playToClient(AbilityDeniedPayload.TYPE, AbilityDeniedPayload.STREAM_CODEC);
        registrar.playToClient(AbilityUnlockedPayload.TYPE, AbilityUnlockedPayload.STREAM_CODEC);
    }
}
