package com.toddlandis.skillcraft.prayer;

import java.util.ArrayList;
import java.util.List;

import com.toddlandis.skillcraft.SkillCraft;
import com.toddlandis.skillcraft.ability.AbilityDefinition;
import com.toddlandis.skillcraft.ability.AbilityManager;
import com.toddlandis.skillcraft.ability.AbilityRuntime;
import com.toddlandis.skillcraft.ability.behavior.Blessing;
import com.toddlandis.skillcraft.combat.CombatDamage;
import com.toddlandis.skillcraft.combat.DamageStyle;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

// Prayer in play (docs/AGILITY_MAGIC_PRAYER.md section 3):
//   - active blessings change damage dealt and taken (after the combat triangle)
//   - Saving Grace turns a killing blow into a narrow escape, spending all Faith
//   - sneak + use with an offering buries it for Prayer XP
//   - Faith is synced on login and refilled on respawn
@EventBusSubscriber(modid = SkillCraft.MODID)
public final class PrayerEvents {
    private PrayerEvents() {}

    // The blessings a player has switched on
    public static List<Blessing> active(ServerPlayer player) {
        List<Blessing> blessings = new ArrayList<>();
        for (Identifier id : AbilityRuntime.of(player).toggles) {
            AbilityDefinition def = AbilityManager.get(id);
            if (def != null && def.behavior() instanceof Blessing blessing) blessings.add(blessing);
        }
        return blessings;
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide()) return;
        DamageSource source = event.getSource();
        Entity attacker = source.getEntity();
        if (attacker == null || attacker == victim) return; // only damage from creatures
        double multiplier = damageMultiplier(victim, attacker, CombatDamage.currentStyle(source));
        if (multiplier != 1.0) event.setAmount((float) Math.max(0, event.getAmount() * multiplier));
    }

    // What active blessings do to one hit: the defender's guard and wards, the attacker's boosts
    public static double damageMultiplier(LivingEntity victim, Entity attacker, DamageStyle style) {
        double multiplier = 1.0;
        if (victim instanceof ServerPlayer defender) {
            for (Blessing blessing : active(defender)) {
                multiplier *= 1 + blessing.damageTaken();
                if (style != null) multiplier *= 1 - blessing.protection(style.discipline());
            }
        }
        if (attacker instanceof ServerPlayer striker && style != null) {
            for (Blessing blessing : active(striker)) multiplier *= 1 + blessing.dealtBonus(style.discipline());
        }
        return multiplier;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    static void onDamagePre(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getNewDamage() < player.getHealth()) return;
        if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return; // the void, /kill
        float grace = 0;
        for (Blessing blessing : active(player)) grace = Math.max(grace, blessing.savingGrace());
        if (grace <= 0) return;
        event.setNewDamage(0);
        player.setHealth(Math.max(player.getHealth(), player.getMaxHealth() * grace));
        PrayerService.set(player, 0); // the blessings switch off next tick
        player.level().sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1, player.getZ(), 40, 0.5, 0.8, 0.5, 0.3);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.8f, 1.2f);
        player.sendOverlayMessage(Component.translatable("message.skillcraft.saving_grace").withStyle(ChatFormatting.GOLD));
    }

    @SubscribeEvent
    static void onUseItem(PlayerInteractEvent.RightClickItem event) {
        ItemStack stack = event.getItemStack();
        if (!event.getEntity().isShiftKeyDown() || PrayerService.offering(stack) == null) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (event.getEntity() instanceof ServerPlayer player) PrayerService.offer(player, stack, 1, false);
    }

    @SubscribeEvent
    static void onDeath(net.neoforged.neoforge.event.entity.living.LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) com.toddlandis.skillcraft.ability.AbilityService.stopAllToggles(player);
    }

    @SubscribeEvent
    static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) PrayerService.save(player, PrayerService.faith(player));
    }

    @SubscribeEvent
    static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) PrayerService.restore(player);
    }
}
