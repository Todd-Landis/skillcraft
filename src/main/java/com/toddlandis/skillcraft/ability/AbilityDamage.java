package com.toddlandis.skillcraft.ability;

import com.toddlandis.skillcraft.combat.CombatDamage;
import com.toddlandis.skillcraft.combat.DamageStyle;
import com.toddlandis.skillcraft.combat.DamageStyles;
import com.toddlandis.skillcraft.magic.MagicFocus;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

// Ability damage. Every hit goes through the Phase 5 pipeline (CombatDamage), so type matchups,
// Sundered, level bonuses, attack styles, typed armour and combat XP all apply exactly as for a
// normal hit. Abilities never give XP of their own: the XP is the usual XP for the damage dealt.
//   - Damage is a share of the held weapon's normal, fully charged hit (spec 3), so it scales with gear.
//     Magic damage (fire, frost, storm, arcane) is a share of the main-hand focus's power instead.
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

    // The off-hand item's hit, as if it were swung in the main hand: the attack damage attribute
    // (base damage, Strength and so on) minus the main-hand weapon's bonus plus the off-hand item's
    public static double offhandHit(Player player) {
        return Math.max(1, weaponHit(player) - attackBonus(player.getMainHandItem()) + attackBonus(player.getOffhandItem()));
    }

    // The flat attack damage an item adds when held in the main hand (+6 for a diamond sword)
    public static double attackBonus(ItemStack stack) {
        double[] bonus = {0};
        stack.forEachModifier(EquipmentSlot.MAINHAND, (attribute, modifier) -> {
            if (attribute.value() == Attributes.ATTACK_DAMAGE.value() && modifier.operation() == AttributeModifier.Operation.ADD_VALUE) {
                bonus[0] += modifier.amount();
            }
        });
        return bonus[0];
    }

    // What a share of 100% means for this damage type: the focus's power for magic, else the weapon's hit
    public static double basePower(Player player, DamageStyle style) {
        if (style != null && style.isMagic()) {
            MagicFocus focus = MagicFocus.held(player);
            return focus != null ? focus.power() : 1.0;
        }
        return weaponHit(player);
    }

    // The type an ability hit has when its JSON doesn't say: a held focus's element, else the weapon's type
    public static DamageStyle defaultStyle(Player player) {
        MagicFocus focus = MagicFocus.held(player);
        return focus != null ? focus.elementOrArcane() : DamageStyles.weapon(player.getMainHandItem());
    }

    // Deals `share` x the base power as `style` (null = the held weapon's or focus's own type)
    public static boolean strike(ServerPlayer player, LivingEntity target, double share, DamageStyle style) {
        DamageStyle type = style != null ? style : defaultStyle(player);
        return hurt(player, target, (float) (basePower(player, type) * share), type);
    }

    // Deals a flat amount through the pipeline (used by Bleed and arrow piercing)
    public static boolean hurt(ServerPlayer player, LivingEntity target, float amount, DamageStyle style) {
        if (amount <= 0 || !CrowdControl.canAffect(player, target)) return false;
        if (target instanceof Player) amount /= 2; // PvP: halved (spec 2.7)
        ServerLevel level = player.level();
        DamageStyle type = style != null ? style : defaultStyle(player);
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
