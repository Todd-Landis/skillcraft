package com.toddlandis.skillcraft.combat;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

import com.toddlandis.skillcraft.SkillCraft;
import com.toddlandis.skillcraft.registry.ModAttachments;
import com.toddlandis.skillcraft.registry.ModDataMaps;
import com.toddlandis.skillcraft.registry.ModEffects;
import com.toddlandis.skillcraft.skill.Skill;
import com.toddlandis.skillcraft.skill.SkillManager;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

// Applies the combat triangle to every hit from an entity (see docs/COMBAT.md):
//   attacker side: monster weakness/resistance, Melee/Ranged level bonus, attack style, Sundered
//   defender side (players): typed armour defence, Defence level, Defensive style
// Untyped damage (falls, fire, explosions...) is left alone.
@EventBusSubscriber(modid = SkillCraft.MODID)
public final class CombatDamage {
    public static final double LEVEL_DAMAGE_BONUS = 0.0025;   // per Melee/Ranged level above 1
    public static final double LEVEL_DEFENCE_BONUS = 0.0015;  // per Defence level above 1
    public static final double SUNDERED_BONUS = 1.15;
    public static final double EFFECTIVE = 1.15, RESISTED = 0.85;
    private static final double MAX_TYPED_DEFENCE = 40, MIN_TYPED_DEFENCE = -25;

    // Set while a scripted attack (e.g. a boss slam) deals damage, so its type is known
    private static DamageStyle pendingStyle;
    private static final Map<UUID, Long> LAST_FEEDBACK = new WeakHashMap<>();

    private CombatDamage() {}

    // Deals damage that counts as the given damage type (used by boss attacks)
    public static boolean hurtWithStyle(ServerLevel level, LivingEntity target, DamageSource source, float amount, DamageStyle style) {
        DamageStyle previous = pendingStyle;
        pendingStyle = style;
        try {
            return target.hurtServer(level, source, amount);
        } finally {
            pendingStyle = previous;
        }
    }

    // The damage type of a hit being processed right now (a scripted style if one is set)
    public static DamageStyle currentStyle(DamageSource source) {
        return pendingStyle != null ? pendingStyle : DamageStyles.of(source);
    }

    @SubscribeEvent
    static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (!(target.level() instanceof ServerLevel level)) return;
        DamageSource source = event.getSource();
        Entity attacker = source.getEntity();
        double multiplier = 1.0;

        if (target.hasEffect(ModEffects.SUNDERED)) multiplier *= SUNDERED_BONUS;

        DamageStyle style = pendingStyle != null ? pendingStyle : DamageStyles.of(source);
        if (attacker != null && attacker != target && style != null) {
            // Attacker side
            if (!(target instanceof Player)) {
                double matchup = DamageStyles.modifier(target, style);
                multiplier *= matchup;
                if (attacker instanceof ServerPlayer player) feedback(player, level, target, style, matchup);
            }
            if (attacker instanceof ServerPlayer player) {
                Skill skill = style.isRanged() ? Skill.RANGED : Skill.MELEE;
                multiplier *= 1 + LEVEL_DAMAGE_BONUS * (SkillManager.level(player, skill) - 1);
                multiplier *= player.getData(ModAttachments.ATTACK_STYLE).damageDealtMultiplier();
            }
            // Defender side
            if (target instanceof ServerPlayer victim) {
                multiplier *= 1 - typedDefence(victim, style.group()) / 100.0;
                multiplier *= 1 - LEVEL_DEFENCE_BONUS * (SkillManager.level(victim, Skill.DEFENCE) - 1);
                multiplier *= victim.getData(ModAttachments.ATTACK_STYLE).damageTakenMultiplier();
            }
        }
        if (multiplier != 1.0) event.setAmount((float) (event.getAmount() * multiplier));
    }

    // Sum of the worn armour's defence against a damage group, clamped to -25..+40 percent
    public static double typedDefence(LivingEntity entity, DamageStyle.Group group) {
        double total = 0;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR) continue;
            ItemStack stack = entity.getItemBySlot(slot);
            ArmorDefence defence = stack.isEmpty() ? null : stack.typeHolder().getData(ModDataMaps.ARMOR_DEFENCES);
            if (defence != null) total += defence.get(group);
        }
        return Math.clamp(total, MIN_TYPED_DEFENCE, MAX_TYPED_DEFENCE);
    }

    // "Effective!" / "Resisted" sparks, sound and (at most once a second) an overlay message
    private static void feedback(ServerPlayer player, ServerLevel level, LivingEntity target, DamageStyle style, double matchup) {
        if (matchup < EFFECTIVE && matchup > RESISTED) return;
        boolean effective = matchup >= EFFECTIVE;
        double y = target.getY() + target.getBbHeight() * 0.6;
        if (effective) {
            level.sendParticles(ParticleTypes.ENCHANTED_HIT, target.getX(), y, target.getZ(), 12, 0.3, 0.3, 0.3, 0.2);
            level.playSound(null, target.getX(), y, target.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8f, 1.6f);
        } else {
            level.sendParticles(ParticleTypes.SMOKE, target.getX(), y, target.getZ(), 6, 0.2, 0.2, 0.2, 0.02);
            level.playSound(null, target.getX(), y, target.getZ(), SoundEvents.SHIELD_BLOCK.value(), SoundSource.PLAYERS, 0.4f, 0.7f);
        }
        long now = level.getGameTime();
        Long last = LAST_FEEDBACK.get(player.getUUID());
        if (last != null && now - last < 20) return;
        LAST_FEEDBACK.put(player.getUUID(), now);
        String percent = String.format("%d%%", Math.round(matchup * 100));
        player.sendOverlayMessage(Component.translatable(effective ? "message.skillcraft.hit_effective" : "message.skillcraft.hit_resisted",
                style.displayName(), percent).withStyle(effective ? ChatFormatting.GREEN : ChatFormatting.GRAY));
    }
}
