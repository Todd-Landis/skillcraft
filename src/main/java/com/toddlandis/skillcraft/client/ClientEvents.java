package com.toddlandis.skillcraft.client;

import java.util.List;
import java.util.Map;

import com.mojang.blaze3d.platform.InputConstants;
import com.toddlandis.skillcraft.SkillCraft;
import com.toddlandis.skillcraft.network.XpGainPayload;
import com.toddlandis.skillcraft.registry.ModDataMaps;
import com.toddlandis.skillcraft.skill.Skill;
import com.toddlandis.skillcraft.skill.SkillManager;
import com.toddlandis.skillcraft.skill.SkillRequirements;
import com.toddlandis.skillcraft.skill.SkillResource;

import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

// All client-only hooks: key binding, HUD overlay, packet handling and tooltips.
@EventBusSubscriber(modid = SkillCraft.MODID, value = Dist.CLIENT)
public class ClientEvents {
    private static final KeyMapping.Category CATEGORY = new KeyMapping.Category(Identifier.fromNamespaceAndPath(SkillCraft.MODID, "main"));
    public static final KeyMapping OPEN_SKILLS = new KeyMapping("key.skillcraft.open_skills", InputConstants.KEY_K, CATEGORY);

    @SubscribeEvent
    static void registerKeys(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        event.register(OPEN_SKILLS);
    }

    @SubscribeEvent
    static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(Identifier.fromNamespaceAndPath(SkillCraft.MODID, "xp_drops"), XpDropOverlay::render);
    }

    @SubscribeEvent
    static void registerPayloadHandlers(RegisterClientPayloadHandlersEvent event) {
        event.register(XpGainPayload.TYPE, (payload, context) -> {
            XpDropOverlay.add(payload);
            if (payload.newLevel() > 0 && context.player() != null) {
                context.player().playSound(SoundEvents.PLAYER_LEVELUP, 0.6f, 1.0f);
            }
        });
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        while (OPEN_SKILLS.consumeClick()) {
            if (mc.player != null && mc.gui.screen() == null) mc.gui.setScreen(new SkillsScreen());
        }
    }

    // Adds "Requires Mining 30" lines to gated items, and gathering info to resource blocks
    @SubscribeEvent
    static void onTooltip(ItemTooltipEvent event) {
        Player player = event.getEntity();
        if (player == null) return;
        List<Component> tooltip = event.getToolTip();

        SkillRequirements req = SkillManager.requirements(event.getItemStack());
        if (req != null) {
            for (Map.Entry<Skill, Integer> entry : req.levels().entrySet()) {
                boolean met = SkillManager.level(player, entry.getKey()) >= entry.getValue();
                tooltip.add(Component.translatable("tooltip.skillcraft.requires", entry.getKey().displayName(), entry.getValue())
                        .withStyle(met ? ChatFormatting.GREEN : ChatFormatting.RED));
            }
        }

        if (event.getItemStack().getItem() instanceof BlockItem blockItem) {
            SkillResource resource = blockItem.getBlock().defaultBlockState().typeHolder().getData(ModDataMaps.BLOCK_RESOURCES);
            if (resource != null) {
                boolean met = SkillManager.level(player, resource.skill()) >= resource.level();
                tooltip.add(Component.translatable("tooltip.skillcraft.resource", resource.skill().displayName(), resource.level(), resource.xp())
                        .withStyle(met ? ChatFormatting.GRAY : ChatFormatting.RED));
            }
        }
    }
}
