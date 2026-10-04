package com.toddlandis.skillcraft.arena;

import com.toddlandis.skillcraft.SkillCraft;
import com.toddlandis.skillcraft.boss.SlagheartEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

// Arena entry/exit rules that keep players from ever getting stuck:
//   - dying in the arena: the death is cancelled; you keep your items, lose the attempt and are sent home
//   - logging out mid-fight: you leave the instance at once and are sent home at your next login
//   - leaving the dimension any other way (commands): you're taken out of the instance
// Cinder Mites never drop anything.
@EventBusSubscriber(modid = SkillCraft.MODID)
public final class ArenaEvents {
    private ArenaEvents() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !ArenaDimension.is(player.level())) return;
        event.setCanceled(true);
        player.setHealth(player.getMaxHealth());
        player.removeAllEffects();
        player.clearFire();
        player.setDeltaMovement(0, 0, 0);
        player.fallDistance = 0;
        player.sendSystemMessage(Component.translatable("message.skillcraft.arena_defeated").withStyle(ChatFormatting.RED));
        ArenaManager.get(player.level().getServer()).leave(player, ArenaManager.LeaveReason.DEATH);
    }

    @SubscribeEvent
    static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ArenaManager manager = ArenaManager.get(player.level().getServer());
            if (manager.isInInstance(player)) manager.leave(player, ArenaManager.LeaveReason.LOGOUT);
        }
    }

    // Anyone who logs in inside the arena (after a logout, crash or restart) goes back home
    @SubscribeEvent
    static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && ArenaDimension.is(player.level())
                && !ArenaManager.get(player.level().getServer()).isInInstance(player)) {
            player.sendSystemMessage(Component.translatable("message.skillcraft.arena_returned").withStyle(ChatFormatting.GRAY));
            ArenaManager.sendHome(player);
        }
    }

    @SubscribeEvent
    static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && ArenaDimension.isKey(player.level().getServer(), event.getFrom())) {
            ArenaManager manager = ArenaManager.get(player.level().getServer());
            if (manager.isInInstance(player)) manager.leave(player, ArenaManager.LeaveReason.ELSEWHERE);
        }
    }

    @SubscribeEvent
    static void onDrops(LivingDropsEvent event) {
        if (event.getEntity() instanceof Mob mob && mob.entityTags().contains(SlagheartEntity.MITE_TAG)) event.setCanceled(true);
    }

    // Tells server admins whether the arena dimension exists (it comes from the mod's datapack)
    @SubscribeEvent
    static void onServerStarted(ServerStartedEvent event) {
        if (event.getServer().getLevel(ArenaDimension.LEVEL) != null) SkillCraft.LOGGER.info("Arena dimension {} loaded", ArenaDimension.LEVEL.identifier());
        else SkillCraft.LOGGER.warn("Arena dimension {} is not loaded; bosses can't be fought", ArenaDimension.LEVEL.identifier());
    }

    @SubscribeEvent
    static void onServerTick(ServerTickEvent.Post event) {
        ArenaManager.get(event.getServer()).tick(event.getServer());
    }
}
