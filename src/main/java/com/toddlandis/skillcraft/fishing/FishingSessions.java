package com.toddlandis.skillcraft.fishing;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.toddlandis.skillcraft.SkillCraft;
import com.toddlandis.skillcraft.node.GatheringSessions;
import com.toddlandis.skillcraft.node.NodeType;
import com.toddlandis.skillcraft.registry.ModDataMaps;
import com.toddlandis.skillcraft.skill.GatheringTool;
import com.toddlandis.skillcraft.skill.Skill;
import com.toddlandis.skillcraft.skill.SkillManager;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

// OSRS-style auto-fishing: right-click a fishing spot with a rod once, and the player keeps
// fishing while they stay near it and hold the rod. Same rules as nodes (see GatheringSessions):
// success chance from level and rod power, full XP per catch, level-scaled depletion, and
// gathering fatigue. Using the rod again (reeling in) or clicking the spot again stops.
//
// Rods: tier and power come from gathering_tools.json like axes and pickaxes. Lure adds one tier
// per level (faster attempts); Luck of the Sea adds +0.05 power per level (more bites).
@EventBusSubscriber(modid = SkillCraft.MODID)
public final class FishingSessions {
    // An attempt every 1.5s with a plain rod, 0.1s faster per tier, never faster than 0.8s
    private static final int BASE_ATTEMPT_TICKS = 30;
    private static final int TICKS_PER_TIER = 2;
    private static final int MIN_ATTEMPT_TICKS = 16;
    private static final double MAX_DISTANCE_SQ = 6.5 * 6.5;
    private static final double LUCK_POWER_PER_LEVEL = 0.05;

    private record Session(FishingSpotEntity spot, FishingLine line, long nextAttempt) {}

    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    private FishingSessions() {}

    public static int attemptTicks(int tier) {
        return Math.max(MIN_ATTEMPT_TICKS, BASE_ATTEMPT_TICKS - TICKS_PER_TIER * tier);
    }

    public static void start(ServerPlayer player, FishingSpotEntity spot) {
        NodeType type = spot.type();
        Session current = SESSIONS.get(player.getUUID());
        if (current != null && current.spot() == spot) {
            stop(player, "message.skillcraft.fishing_stop"); // clicking the spot again stops
            return;
        }
        if (spot.isDepleted()) return;
        String problem = checkCanFish(player, type);
        if (problem != null) {
            player.sendOverlayMessage(Component.translatable(problem, type.skill().displayName(), type.level()).withStyle(ChatFormatting.RED));
            return;
        }
        if (GatheringSessions.isFatigued(player, type)) {
            GatheringSessions.sendFatigued(player, type);
            return;
        }
        if (current != null) endQuietly(player);
        if (player.fishing != null) player.fishing.discard(); // a vanilla bobber already out

        FishingLine line = FishingLine.cast(player, spot);
        SESSIONS.put(player.getUUID(), new Session(spot, line, player.level().getGameTime() + attemptTicks(player)));
        player.swing(InteractionHand.MAIN_HAND, true);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FISHING_BOBBER_THROW, SoundSource.NEUTRAL,
                0.5f, 0.4f / (player.getRandom().nextFloat() * 0.4f + 0.8f));
        player.sendOverlayMessage(Component.translatable("message.skillcraft.fishing_start"));
    }

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) tick(player);
    }

    // Public so GameTests can drive fake players, which are never ticked
    public static void tick(ServerPlayer player) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null) return;
        FishingSpotEntity spot = session.spot();
        if (spot.isRemoved() || spot.isDepleted() || spot.level() != player.level()) {
            endQuietly(player);
            return;
        }
        if (session.line().isRemoved()) {
            SESSIONS.remove(player.getUUID()); // reeled in or unloaded
            return;
        }
        if (player.distanceToSqr(spot) > MAX_DISTANCE_SQ) {
            stop(player, "message.skillcraft.gather_too_far");
            return;
        }
        NodeType type = spot.type();
        if (checkCanFish(player, type) != null) {
            stop(player, "message.skillcraft.fishing_stop");
            return;
        }
        ServerLevel level = player.level();
        if (level.getGameTime() < session.nextAttempt()) return;
        if (GatheringSessions.isFatigued(player, type)) {
            endQuietly(player);
            GatheringSessions.sendFatigued(player, type);
            return;
        }
        SESSIONS.put(player.getUUID(), new Session(spot, session.line(), level.getGameTime() + attemptTicks(player)));
        attempt(player, level, spot, session.line());
    }

    private static void attempt(ServerPlayer player, ServerLevel level, FishingSpotEntity spot, FishingLine line) {
        NodeType type = spot.type();
        int fishingLevel = SkillManager.level(player, Skill.FISHING);
        if (level.getRandom().nextDouble() >= type.successChance(fishingLevel, rodPower(player))) return; // no bite this time

        ItemStack fish = new ItemStack(type.drop(), type.dropCount());
        if (!player.getInventory().add(fish)) player.drop(fish, false);
        SkillManager.awardXp(player, Skill.FISHING, type.xp());
        GatheringSessions.addFatigue(player, type);
        line.dip();
        player.swing(InteractionHand.MAIN_HAND, true);
        double y = spot.waterPos().getY() + FishingSpotEntity.SURFACE_Y;
        level.playSound(null, spot.getX(), y, spot.getZ(), SoundEvents.FISHING_BOBBER_SPLASH, SoundSource.NEUTRAL, 0.6f, 0.9f + level.getRandom().nextFloat() * 0.3f);
        level.sendParticles(ParticleTypes.SPLASH, line.getX(), y, line.getZ(), 12, 0.15, 0.05, 0.15, 0.1);
        level.sendParticles(ParticleTypes.FISHING, line.getX(), y, line.getZ(), 6, 0.1, 0.0, 0.1, 0.05);

        if (level.getRandom().nextDouble() < type.depleteChance(fishingLevel)) {
            spot.deplete(level);
            stop(player, "message.skillcraft.shoal_swam_off");
        }
    }

    // Returns a translation key describing why the player can't fish here, or null if they can
    private static String checkCanFish(ServerPlayer player, NodeType type) {
        if (SkillManager.level(player, Skill.FISHING) < type.level()) return "message.skillcraft.resource_level";
        ItemStack rod = player.getMainHandItem();
        if (!rod.canPerformAction(ItemAbilities.FISHING_ROD_CAST)) return "message.skillcraft.need_rod";
        if (SkillManager.missingLevel(player, rod, Skill.FISHING) > 0) return "message.skillcraft.tool_too_advanced";
        return null;
    }

    private static int attemptTicks(ServerPlayer player) {
        ItemStack rod = player.getMainHandItem();
        return attemptTicks(ModDataMaps.gatheringTool(rod).tier() + enchantLevel(player, rod, Enchantments.LURE));
    }

    private static double rodPower(ServerPlayer player) {
        ItemStack rod = player.getMainHandItem();
        GatheringTool tool = ModDataMaps.gatheringTool(rod);
        return tool.power() + LUCK_POWER_PER_LEVEL * enchantLevel(player, rod, Enchantments.LUCK_OF_THE_SEA);
    }

    private static int enchantLevel(ServerPlayer player, ItemStack stack, ResourceKey<Enchantment> key) {
        Holder<Enchantment> enchantment = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
        return EnchantmentHelper.getItemEnchantmentLevel(enchantment, stack);
    }

    public static boolean isFishing(ServerPlayer player) {
        return SESSIONS.containsKey(player.getUUID());
    }

    static boolean isLine(ServerPlayer player, FishingLine line) {
        Session session = SESSIONS.get(player.getUUID());
        return session != null && session.line() == line;
    }

    public static void stop(ServerPlayer player, String messageKey) {
        if (!SESSIONS.containsKey(player.getUUID())) return;
        endQuietly(player);
        player.sendOverlayMessage(Component.translatable(messageKey).withStyle(ChatFormatting.GRAY));
    }

    private static void endQuietly(ServerPlayer player) {
        Session session = SESSIONS.remove(player.getUUID());
        if (session != null && !session.line().isRemoved()) session.line().discard();
    }

    @SubscribeEvent
    static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) endQuietly(player);
    }
}
