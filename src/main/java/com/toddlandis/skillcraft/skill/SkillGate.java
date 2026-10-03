package com.toddlandis.skillcraft.skill;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

// Shows "you need level X" messages above the hotbar, at most once a second per player,
// since the events that trigger them fire every tick.
public final class SkillGate {
    private static final Map<UUID, Long> LAST_MESSAGE = new WeakHashMap<>();
    private static final long COOLDOWN_TICKS = 20;

    private SkillGate() {}

    public static void deny(Player player, String translationKey, Skill skill, int level) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        long now = serverPlayer.level().getGameTime();
        Long last = LAST_MESSAGE.get(player.getUUID());
        if (last != null && now - last < COOLDOWN_TICKS) return;
        LAST_MESSAGE.put(player.getUUID(), now);
        serverPlayer.sendOverlayMessage(Component.translatable(translationKey, skill.displayName(), level).withStyle(ChatFormatting.RED));
    }
}
