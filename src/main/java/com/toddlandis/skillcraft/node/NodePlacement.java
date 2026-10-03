package com.toddlandis.skillcraft.node;

import com.toddlandis.skillcraft.SkillCraft;
import com.toddlandis.skillcraft.altar.AttunementAltarBlock;
import com.toddlandis.skillcraft.registry.ModPoiTypes;
import com.toddlandis.skillcraft.skill.SkillManager;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

// Enforces node spacing: at most NodeType.spacingMax(level) nodes of the same type within
// NodeType.spacingRadius() blocks. Higher tiers must be spread further apart; the placing
// player's level in that skill unlocks a few extra nodes per area.
@EventBusSubscriber(modid = SkillCraft.MODID)
public final class NodePlacement {
    private NodePlacement() {}

    @SubscribeEvent
    static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getPlacedBlock().getBlock() instanceof NodeBlock node)) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        NodeType type = node.type();
        BlockPos pos = event.getPos();
        var poiKey = ModPoiTypes.NODES.get(type).getKey();
        long nearby = level.getPoiManager()
                .getInRange(holder -> holder.is(poiKey), pos, type.spacingRadius(), PoiManager.Occupancy.ANY)
                .filter(record -> !record.getPos().equals(pos))
                .count();

        int playerLevel = event.getEntity() instanceof ServerPlayer p ? SkillManager.level(p, type.skill()) : type.level();
        int allowed = type.spacingMax(playerLevel);
        if (nearby >= allowed) {
            event.setCanceled(true);
            if (event.getEntity() instanceof ServerPlayer player) {
                player.sendOverlayMessage(Component.translatable("message.skillcraft.node_too_close",
                        AttunementAltarBlock.nodeName(type), allowed, type.spacingRadius()).withStyle(ChatFormatting.RED));
            }
        }
    }
}
