package com.toddlandis.skillcraft.ability;

import com.toddlandis.skillcraft.Config;
import com.toddlandis.skillcraft.SkillCraft;
import com.toddlandis.skillcraft.combat.BossCrowdControl;
import com.toddlandis.skillcraft.registry.ModEffects;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

// Every stun, root, slow, knockback and pull from an ability goes through here, so the boss and
// PvP rules (spec 2.6, 2.7) can't be skipped:
//   bosses: stun -> Daze (next attack +1 s, at most once per 20 s), root/slow -> at most 30% slow
//           for half the time, knockback/pull -> none
//   players (only if abilitiesAffectPlayers is on): durations halved, roots/stuns at most 1 s
//   Unshackled: immune to slows, roots and stuns
public final class CrowdControl {
    public static final TagKey<EntityType<?>> BOSSES = TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(SkillCraft.MODID, "bosses"));
    public static final int DAZE_TICKS = 20;
    public static final int DAZE_LOCKOUT = 400; // at most one Daze per boss every 20 s
    public static final int BOSS_SLOW_LEVELS = 3; // Slowed III = -30%

    private CrowdControl() {}

    public static boolean isBoss(Entity entity) {
        return net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(entity.getType()).is(BOSSES);
    }

    // Can this player's abilities affect the target at all?
    public static boolean canAffect(Player caster, Entity target) {
        if (target == caster || !(target instanceof LivingEntity living) || !living.isAlive()) return false;
        if (target instanceof Player other) return Config.ABILITIES_AFFECT_PLAYERS.get() && caster.canHarmPlayer(other);
        if (target instanceof Mob mob && mob.isAlliedTo(caster)) return false;
        return !living.isInvulnerable() && living.isAttackable();
    }

    public static boolean isControlEffect(Holder<MobEffect> effect) {
        return same(effect, ModEffects.ROOTED) || same(effect, ModEffects.SLOWED) || same(effect, ModEffects.STUNNED) || same(effect, MobEffects.SLOWNESS);
    }

    private static boolean same(Holder<MobEffect> a, Holder<MobEffect> b) {
        return a.value() == b.value();
    }

    // Applies a debuff with the boss / PvP / Unshackle rules. Returns true if something was applied.
    public static boolean apply(LivingEntity target, Holder<MobEffect> effect, int duration, int amplifier, Entity source) {
        if (duration <= 0) return false;
        boolean control = isControlEffect(effect);
        if (control && target.hasEffect(ModEffects.UNSHACKLED)) return false;
        if (isBoss(target)) {
            if (same(effect, ModEffects.STUNNED)) return daze(target);
            if (control) {
                // Roots and slows become a capped slow for half the time
                int levels = same(effect, ModEffects.SLOWED) ? Math.min(amplifier + 1, BOSS_SLOW_LEVELS) : BOSS_SLOW_LEVELS;
                return target.addEffect(new MobEffectInstance(ModEffects.SLOWED, Math.max(1, duration / 2), levels - 1), source);
            }
        }
        if (target instanceof Player) {
            duration = Math.max(1, duration / 2);
            if (same(effect, ModEffects.ROOTED) || same(effect, ModEffects.STUNNED)) duration = Math.min(duration, 20);
        }
        return target.addEffect(new MobEffectInstance(effect, duration, amplifier), source);
    }

    public static boolean stun(LivingEntity target, int duration, Entity source) {
        return apply(target, ModEffects.STUNNED, duration, 0, source);
    }

    // Boss Daze: delegated to the boss so it can delay its next attack without touching a windup
    public static boolean daze(LivingEntity boss) {
        boolean took = boss instanceof BossCrowdControl control && control.daze(DAZE_TICKS);
        if (took) boss.addEffect(new MobEffectInstance(ModEffects.DAZED, DAZE_TICKS, 0));
        return took;
    }

    // Horizontal shove away from the caster (none on bosses; halved on players)
    public static void knockback(LivingEntity target, Entity from, double strength) {
        if (strength <= 0 || isBoss(target)) return;
        if (target instanceof Player) strength /= 2;
        Vec3 away = target.position().subtract(from.position()).multiply(1, 0, 1);
        if (away.lengthSqr() < 1.0E-4) away = from.getLookAngle().multiply(1, 0, 1);
        away = away.normalize().scale(strength * Math.max(0, 1 - target.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE)));
        target.push(away.x, 0.15 * strength, away.z);
        target.hurtMarked = true;
    }
}
