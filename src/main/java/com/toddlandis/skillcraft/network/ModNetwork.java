package com.toddlandis.skillcraft.network;

import com.toddlandis.skillcraft.SkillCraft;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@EventBusSubscriber(modid = SkillCraft.MODID)
public class ModNetwork {
    @SubscribeEvent
    static void register(RegisterPayloadHandlersEvent event) {
        // The handler for client-bound packets is registered in client/ClientEvents
        event.registrar("1").playToClient(XpGainPayload.TYPE, XpGainPayload.STREAM_CODEC);
    }
}
