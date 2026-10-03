package com.toddlandis.skillcraft.network;

import com.toddlandis.skillcraft.SkillCraft;
import com.toddlandis.skillcraft.altar.AltarAttunement;

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
    }
}
