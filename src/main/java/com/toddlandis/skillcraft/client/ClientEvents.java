package com.toddlandis.skillcraft.client;

import java.util.List;
import java.util.Map;

import com.mojang.blaze3d.platform.InputConstants;
import com.toddlandis.skillcraft.SkillCraft;
import com.toddlandis.skillcraft.altar.AttunementAltarBlock;
import com.toddlandis.skillcraft.bench.BenchRecipe;
import com.toddlandis.skillcraft.client.boss.SlagheartModel;
import com.toddlandis.skillcraft.client.boss.SlagheartRenderer;
import com.toddlandis.skillcraft.combat.ArmorDefence;
import com.toddlandis.skillcraft.combat.DamageStyle;
import com.toddlandis.skillcraft.combat.WeaponStyle;
import com.toddlandis.skillcraft.loot.EarnedBy;
import com.toddlandis.skillcraft.network.CycleAttackStylePayload;
import com.toddlandis.skillcraft.registry.ModEntities;
import com.toddlandis.skillcraft.network.BankSyncPayload;
import com.toddlandis.skillcraft.network.OpenAttunementPayload;
import com.toddlandis.skillcraft.network.XpGainPayload;
import com.toddlandis.skillcraft.node.NodeBlock;
import com.toddlandis.skillcraft.node.NodeType;
import com.toddlandis.skillcraft.registry.ModDataComponents;
import com.toddlandis.skillcraft.registry.ModMenuTypes;
import com.toddlandis.skillcraft.skill.GatheringTool;
import com.toddlandis.skillcraft.skill.SmeltingRecipe;
import com.toddlandis.skillcraft.skill.XpTable;
import com.toddlandis.skillcraft.registry.ModDataMaps;
import com.toddlandis.skillcraft.skill.Skill;
import com.toddlandis.skillcraft.skill.SkillManager;
import com.toddlandis.skillcraft.skill.SkillRequirements;
import com.toddlandis.skillcraft.skill.SkillResource;

import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

// All client-only hooks: key binding, HUD overlay, packet handling and tooltips.
@EventBusSubscriber(modid = SkillCraft.MODID, value = Dist.CLIENT)
public class ClientEvents {
    private static final KeyMapping.Category CATEGORY = new KeyMapping.Category(Identifier.fromNamespaceAndPath(SkillCraft.MODID, "main"));
    public static final KeyMapping OPEN_SKILLS = new KeyMapping("key.skillcraft.open_skills", InputConstants.KEY_K, CATEGORY);
    public static final KeyMapping ATTACK_STYLE = new KeyMapping("key.skillcraft.attack_style", InputConstants.KEY_V, CATEGORY);

    @SubscribeEvent
    static void registerKeys(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        event.register(OPEN_SKILLS);
        event.register(ATTACK_STYLE);
        com.toddlandis.skillcraft.client.ability.AbilityKeys.register(event);
    }

    // FishingHookRenderer is typed for FishingHook; FishingLine is one, so the cast is safe
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static net.minecraft.client.renderer.entity.EntityRenderer<com.toddlandis.skillcraft.fishing.FishingLine, ?> fishingLineRenderer(
            net.minecraft.client.renderer.entity.EntityRendererProvider.Context context) {
        return (net.minecraft.client.renderer.entity.EntityRenderer) new net.minecraft.client.renderer.entity.FishingHookRenderer(context);
    }

    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.SLAGHEART.get(), SlagheartRenderer::new);
        event.registerEntityRenderer(ModEntities.SLAG_GLOB.get(), context -> new ThrownItemRenderer<>(context, 1.4f, true));
        event.registerEntityRenderer(ModEntities.LIGHT_ARROW.get(), SkillArrowRenderer::new);
        event.registerEntityRenderer(ModEntities.HEAVY_ARROW.get(), SkillArrowRenderer::new);
        event.registerEntityRenderer(ModEntities.FISHING_SPOT.get(), com.toddlandis.skillcraft.client.fishing.FishingSpotRenderer::new);
        // The fishing line is a vanilla hook underneath, so the vanilla bobber-and-line renderer draws it
        event.registerEntityRenderer(ModEntities.FISHING_LINE.get(), ClientEvents::fishingLineRenderer);
    }

    @SubscribeEvent
    static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(SlagheartModel.LAYER, SlagheartModel::createBodyLayer);
    }

    @SubscribeEvent
    static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.SMELTER.get(), SmelterScreen::new);
        event.register(ModMenuTypes.COOKING_RANGE.get(), CookingRangeScreen::new);
        event.register(ModMenuTypes.FORGING_ANVIL.get(), BenchScreen::new);
        event.register(ModMenuTypes.CRAFTING_BENCH.get(), BenchScreen::new);
        event.register(ModMenuTypes.BANK.get(), BankScreen::new);
    }

    @SubscribeEvent
    static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(Identifier.fromNamespaceAndPath(SkillCraft.MODID, "ability_bar"), com.toddlandis.skillcraft.client.ability.AbilityHud::render);
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
        event.register(BankSyncPayload.TYPE, (payload, context) -> ClientBank.apply(payload));
        event.register(com.toddlandis.skillcraft.network.AbilityDefinitionsPayload.TYPE, (payload, context) -> com.toddlandis.skillcraft.client.ability.ClientAbilities.apply(payload));
        event.register(com.toddlandis.skillcraft.network.AbilityCooldownPayload.TYPE, (payload, context) -> com.toddlandis.skillcraft.client.ability.ClientAbilities.apply(payload));
        event.register(com.toddlandis.skillcraft.network.AbilityDeniedPayload.TYPE, (payload, context) -> com.toddlandis.skillcraft.client.ability.ClientAbilities.denied());
        event.register(com.toddlandis.skillcraft.network.AbilityUnlockedPayload.TYPE, (payload, context) -> com.toddlandis.skillcraft.client.ability.ClientAbilities.unlocked(payload));
        event.register(OpenAttunementPayload.TYPE, (payload, context) ->
                Minecraft.getInstance().gui.setScreen(new AttunementScreen(payload)));
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        while (OPEN_SKILLS.consumeClick()) {
            if (mc.player != null && mc.gui.screen() == null) mc.gui.setScreen(new SkillsScreen());
        }
        while (ATTACK_STYLE.consumeClick()) {
            if (mc.player != null && mc.gui.screen() == null) ClientPacketDistributor.sendToServer(new CycleAttackStylePayload());
        }
        com.toddlandis.skillcraft.client.ability.AbilityKeys.handle(mc);
        com.toddlandis.skillcraft.client.ability.ClientAbilities.tick();
    }

    // Adds "Requires Mining 30" lines to gated items, and gathering info to resource blocks
    @SubscribeEvent
    static void onTooltip(ItemTooltipEvent event) {
        Player player = event.getEntity();
        if (player == null) return;
        List<Component> tooltip = event.getToolTip();
        com.toddlandis.skillcraft.client.ability.CodexTooltip.add(event.getItemStack(), player, tooltip);

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
            if (event.getItemStack().canPerformAction(net.neoforged.neoforge.common.ItemAbilities.FISHING_ROD_CAST)) {
                double castSeconds = com.toddlandis.skillcraft.fishing.FishingSessions.attemptTicks(tool.tier()) / 20.0;
                tooltip.add(Component.translatable("tooltip.skillcraft.fishing_tool", XpTable.format(castSeconds), XpTable.format(tool.power()))
                        .withStyle(ChatFormatting.DARK_AQUA));
            } else {
                double swingSeconds = Math.max(8, 15 - tool.tier()) / 20.0;
                tooltip.add(Component.translatable("tooltip.skillcraft.gathering_tool", XpTable.format(swingSeconds), XpTable.format(tool.power()))
                        .withStyle(ChatFormatting.DARK_AQUA));
            }
        }

        if (event.getItemStack().getItem() instanceof com.toddlandis.skillcraft.fishing.ShoalItem shoal) {
            addNodeTooltip(tooltip, player, shoal.type());
            return;
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

        addCombatTooltip(tooltip, event.getItemStack());

        if (event.getItemStack().is(com.toddlandis.skillcraft.registry.ModItems.EMBERFALL_MAUL.get())) {
            tooltip.add(Component.translatable("tooltip.skillcraft.emberfall_maul.sunder").withStyle(ChatFormatting.GOLD));
            tooltip.add(Component.translatable("tooltip.skillcraft.emberfall_maul.weight").withStyle(ChatFormatting.RED));
        } else if (event.getItemStack().is(com.toddlandis.skillcraft.registry.ModItems.EMBER_SIGIL.get())) {
            tooltip.add(Component.translatable("tooltip.skillcraft.ember_sigil").withStyle(ChatFormatting.GOLD));
        } else if (event.getItemStack().is(com.toddlandis.skillcraft.registry.ModItems.SUMMONING_BRAZIER.get())) {
            tooltip.add(Component.translatable("tooltip.skillcraft.summoning_brazier").withStyle(ChatFormatting.GRAY));
        }

        EarnedBy earned = event.getItemStack().get(ModDataComponents.EARNED_BY.get());
        if (earned != null) {
            tooltip.add(Component.translatable("tooltip.skillcraft.earned_by", Component.translatable(earned.source()), earned.player())
                    .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC));
        }

        String maker = event.getItemStack().get(ModDataComponents.CRAFTED_BY.get());
        if (maker != null && !maker.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.skillcraft.crafted_by", maker).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }

        // Gear made at a Forging Anvil or Crafting Bench
        BenchRecipe benchRecipe = BenchRecipe.get(event.getItemStack().getItem());
        if (benchRecipe != null) {
            boolean met = SkillManager.level(player, benchRecipe.station().skill()) >= benchRecipe.level();
            tooltip.add(Component.translatable("tooltip.skillcraft.made_at",
                    Component.translatable("container.skillcraft." + benchRecipe.station().getSerializedName()),
                    benchRecipe.station().skill().displayName(), benchRecipe.level())
                    .withStyle(met ? ChatFormatting.DARK_GREEN : ChatFormatting.DARK_RED));
        }

        // Raw ores the Smelter can process
        SmeltingRecipe smelting = ModDataMaps.smelting(event.getItemStack());
        if (smelting != null) {
            boolean met = SkillManager.level(player, Skill.SMITHING) >= smelting.level();
            tooltip.add(Component.translatable("tooltip.skillcraft.smeltable", smelting.result().getDefaultInstance().getHoverName(),
                    Skill.SMITHING.displayName(), smelting.level(), smelting.coal(), XpTable.format(smelting.xp()))
                    .withStyle(met ? ChatFormatting.GOLD : ChatFormatting.RED));
        }

        // Raw food the Cooking Range can cook, with this player's burn chance
        com.toddlandis.skillcraft.cooking.CookingRecipe cooking = com.toddlandis.skillcraft.cooking.CookingRecipe.get(event.getItemStack());
        if (cooking != null) {
            int cookingLevel = SkillManager.level(player, Skill.COOKING);
            boolean met = cookingLevel >= cooking.level();
            tooltip.add(Component.translatable("tooltip.skillcraft.cookable", cooking.result().getDefaultInstance().getHoverName(),
                    Skill.COOKING.displayName(), cooking.level(), XpTable.format(cooking.xp()))
                    .withStyle(met ? ChatFormatting.GOLD : ChatFormatting.RED));
            if (met) {
                tooltip.add(Component.translatable("tooltip.skillcraft.burn_chance", (int) Math.round(100 * cooking.burnChance(cookingLevel)), cooking.stopBurn())
                        .withStyle(ChatFormatting.GRAY));
            }
        }

        // Resource items that can be attuned into a node at the altar
        NodeType attunable = NodeType.byInput(event.getItemStack().getItem());
        if (attunable != null) {
            tooltip.add(Component.translatable("tooltip.skillcraft.attunable", attunable.inputCount(), AttunementAltarBlock.nodeName(attunable))
                    .withStyle(ChatFormatting.DARK_AQUA));
        }
    }

    // Damage type for weapons and ammo, typed defence for armour (see docs/COMBAT.md)
    private static void addCombatTooltip(List<Component> tooltip, net.minecraft.world.item.ItemStack stack) {
        WeaponStyle weapon = stack.typeHolder().getData(ModDataMaps.WEAPON_STYLES);
        if (weapon != null) {
            tooltip.add(Component.translatable("tooltip.skillcraft.weapon_style", weapon.style().displayName()).withStyle(ChatFormatting.GRAY));
        }
        if (stack.getItem() == com.toddlandis.skillcraft.registry.ModItems.LIGHT_ARROW.get()) {
            tooltip.add(Component.translatable("tooltip.skillcraft.ammo_style", DamageStyle.LIGHT.displayName()).withStyle(ChatFormatting.GRAY));
        } else if (stack.getItem() == com.toddlandis.skillcraft.registry.ModItems.HEAVY_ARROW.get()) {
            tooltip.add(Component.translatable("tooltip.skillcraft.ammo_style", DamageStyle.HEAVY.displayName()).withStyle(ChatFormatting.GRAY));
        } else if (stack.is(net.minecraft.tags.ItemTags.ARROWS)) {
            tooltip.add(Component.translatable("tooltip.skillcraft.ammo_style", DamageStyle.MEDIUM.displayName()).withStyle(ChatFormatting.GRAY));
        }
        ArmorDefence defence = stack.typeHolder().getData(ModDataMaps.ARMOR_DEFENCES);
        if (defence != null) {
            tooltip.add(Component.translatable("tooltip.skillcraft.defence_header").withStyle(ChatFormatting.GRAY));
            for (DamageStyle.Group group : DamageStyle.Group.values()) {
                double value = defence.get(group);
                if (value == 0) continue;
                String text = (value > 0 ? "+" : "") + XpTable.format(value) + "%";
                tooltip.add(Component.literal(" ").append(group.displayName()).append(": " + text)
                        .withStyle(value > 0 ? ChatFormatting.DARK_GREEN : ChatFormatting.DARK_RED));
            }
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
        if (met) {
            int playerLevel = SkillManager.level(player, type.skill());
            tooltip.add(Component.translatable("tooltip.skillcraft.node_fatigue",
                    (int) Math.round(type.fatiguePerHour(playerLevel)), (int) Math.floor(type.fatigueCapacity(playerLevel)))
                    .withStyle(ChatFormatting.GRAY));
        }
        if (type.isSpot()) {
            tooltip.add(Component.translatable("tooltip.skillcraft.shoal_water", type.minWaterArea()).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("tooltip.skillcraft.node_use_rod").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        tooltip.add(Component.translatable(type.skill() == Skill.WOODCUTTING ? "tooltip.skillcraft.node_use_axe" : "tooltip.skillcraft.node_use_pickaxe")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
