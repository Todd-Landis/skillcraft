package com.toddlandis.skillcraft.ability;

import com.toddlandis.skillcraft.combat.CombatDamage;
import com.toddlandis.skillcraft.combat.DamageStyle;
import com.toddlandis.skillcraft.combat.DamageStyles;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

// Ability damage. Every hit goes through the Phase 5 pipeline (CombatDamage), so type matchups,
// Sundered, level bonuses, attack styles, typed armour and combat XP all apply exactly as for a
// normal hit. Abilities never give XP of their own: the XP is the usual XP for the damage dealt.
//   - Damage is a share of the held weapon's normal, fully charged hit (spec 3), so it scales with gear
//   - An ability hit doesn't leave hit-immunity frames behind, so it never eats the next normal swing
public final class AbilityDamage {
    private static int depth;

    private AbilityDamage() {}

    // True while an ability is dealing damage (on-hit Momentum isn't counted for these hits)
    public static boolean active() {
        return depth > 0;
    }

    // The held weapon's normal fully charged hit (attack damage attribute, which includes the weapon)
    public static double weaponHit(Player player) {
        return player.getAttributeValue(Attributes.ATTACK_DAMAGE);
    }

    // Deals `share` x the weapon's hit as `style` (null = the held weapon's own type)
    public static boolean strike(ServerPlayer player, LivingEntity target, double share, DamageStyle style) {
        return hurt(player, target, (float) (weaponHit(player) * share), style);
    }

    // Deals a flat amount through the pipeline (used by Bleed and arrow piercing)
    public static boolean hurt(ServerPlayer player, LivingEntity target, float amount, DamageStyle style) {
        if (amount <= 0 || !CrowdControl.canAffect(player, target)) return false;
        if (target instanceof Player) amount /= 2; // PvP: halved (spec 2.7)
        ServerLevel level = player.level();
        DamageStyle type = style != null ? style : DamageStyles.weapon(player.getMainHandItem());
        int immunity = target.invulnerableTime;
        target.invulnerableTime = 0;
        depth++;
        try {
            return CombatDamage.hurtWithStyle(level, target, player.damageSources().playerAttack(player), amount, type);
        } finally {
            depth--;
            if (target.isAlive()) target.invulnerableTime = immunity;
        }
    }
}
