package com.toddlandis.skillcraft.client;

import java.util.List;
import java.util.Map;

import com.mojang.blaze3d.platform.InputConstants;
import com.toddlandis.skillcraft.SkillCraft;
import com.toddlandis.skillcraft.altar.AttunementAltarBlock;
import com.toddlandis.skillcraft.network.OpenAttunementPayload;
import com.toddlandis.skillcraft.network.XpGainPayload;
import com.toddlandis.skillcraft.node.NodeBlock;
import com.toddlandis.skillcraft.node.NodeType;
import com.toddlandis.skillcraft.skill.GatheringTool;
import com.toddlandis.skillcraft.skill.XpTable;
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
        event.register(OpenAttunementPayload.TYPE, (payload, context) ->
                Minecraft.getInstance().gui.setScreen(new AttunementScreen(payload.pos(), payload.node())));
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

        GatheringTool tool = event.getItemStack().typeHolder().getData(ModDataMaps.GATHERING_TOOLS);
        if (tool != null) {
            double swingSeconds = Math.max(8, 15 - tool.tier()) / 20.0;
            tooltip.add(Component.translatable("tooltip.skillcraft.gathering_tool", XpTable.format(swingSeconds), XpTable.format(tool.power()))
                    .withStyle(ChatFormatting.DARK_AQUA));
        }

        if (event.getItemStack().getItem() instanceof BlockItem blockItem) {
            if (blockItem.getBlock() instanceof NodeBlock node) {
                addNodeTooltip(tooltip, player, node.type());
                return;
            }
            SkillResource resource = blockItem.getBlock().defaultBlockState().typeHolder().getData(ModDataMaps.BLOCK_RESOURCES);
            if (resource != null) {
                boolean met = SkillManager.level(player, resource.skill()) >= resource.level();
                tooltip.add(Component.translatable("tooltip.skillcraft.resource", resource.skill().displayName(), resource.level(),
                        XpTable.tierName(resource.level())).withStyle(met ? ChatFormatting.GRAY : ChatFormatting.RED));
            }
        }

        // Resource items that can be attuned into a node at the altar
        NodeType attunable = NodeType.byInput(event.getItemStack().getItem());
        if (attunable != null) {
            tooltip.add(Component.translatable("tooltip.skillcraft.attunable", attunable.inputCount(), AttunementAltarBlock.nodeName(attunable))
                    .withStyle(ChatFormatting.DARK_AQUA));
        }
    }

    private static void addNodeTooltip(List<Component> tooltip, Player player, NodeType type) {
        boolean met = SkillManager.level(player, type.skill()) >= type.level();
        tooltip.add(Component.translatable("tooltip.skillcraft.resource", type.skill().displayName(), type.level(), type.tierName())
                .withStyle(met ? ChatFormatting.GREEN : ChatFormatting.RED));
        tooltip.add(Component.translatable("tooltip.skillcraft.node_xp", XpTable.format(type.xp()), type.drop().getDefaultInstance().getHoverName())
                .withStyle(ChatFormatting.GRAY));
        if (met) {
            int deplete = (int) Math.round(100 * type.depleteChance(SkillManager.level(player, type.skill())));
            tooltip.add(Component.translatable("tooltip.skillcraft.node_deplete", deplete).withStyle(ChatFormatting.GRAY));
        }
        int regrow = type.regrowSeconds();
        String regrowText = regrow < 60 ? regrow + "s" : (regrow / 60) + "m";
        tooltip.add(Component.translatable("tooltip.skillcraft.node_limits", regrowText, type.spacingMax(), type.spacingRadius())
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(type.skill() == Skill.WOODCUTTING ? "tooltip.skillcraft.node_use_axe" : "tooltip.skillcraft.node_use_pickaxe")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
