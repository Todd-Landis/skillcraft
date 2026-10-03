package com.toddlandis.skillcraft.network;

import com.toddlandis.skillcraft.SkillCraft;
import com.toddlandis.skillcraft.altar.AltarAttunement;
import com.toddlandis.skillcraft.bank.BankActions;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@EventBusSubscriber(modid = SkillCraft.MODID)
public class ModNetwork {
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
    }
}
