package com.toddlandis.skillcraft.ability;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

import com.toddlandis.skillcraft.Config;
import com.toddlandis.skillcraft.SkillCraft;
import com.toddlandis.skillcraft.combat.CombatDamage;
import com.toddlandis.skillcraft.combat.DamageStyle;
import com.toddlandis.skillcraft.combat.SkillArrow;
import com.toddlandis.skillcraft.registry.ModEffects;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.ArrowLooseEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

// Where abilities meet the rest of combat (all server-side):
//   Momentum: +6 x strength^2 per melee hit, +5 per projectile hit, +2 per hit taken (Config);
//             ability damage itself builds none (abilities generate their own fixed amount)
//   Combat state for loadout locks; Riposte parries and counters; Battle Trance, Brace,
//   Bulwark and Last Stand; next-shot bonuses and Deadeye on projectiles; manual arrow piercing;
//   Bleed ticks; stunned mobs can't act; Unshackled immunity.
@EventBusSubscriber(modid = SkillCraft.MODID)
public final class AbilityEvents {
    private static final String SHOT_TAG = "skillcraft.ability_shot";
    private static final String ABILITY_PROJECTILE = "skillcraft.ability_projectile";

    private record Bleed(ServerPlayer source, float perSecond, long until) {}
    private static final Map<LivingEntity, Bleed> BLEEDS = new WeakHashMap<>();
    private static final Map<UUID, Set<Integer>> PIERCED = new HashMap<>();

    private AbilityEvents() {}

    // --- Ticking ---

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        AbilityService.tick(player);
        if (player.hasEffect(ModEffects.BRACED) && player.isSprinting()) player.setSprinting(false);
    }

    @SubscribeEvent
    static void onServerTick(ServerTickEvent.Post event) {
        if (BLEEDS.isEmpty()) return;
        for (Iterator<Map.Entry<LivingEntity, Bleed>> it = BLEEDS.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<LivingEntity, Bleed> entry = it.next();
            LivingEntity target = entry.getKey();
            Bleed bleed = entry.getValue();
            if (!target.isAlive() || !(target.level() instanceof ServerLevel level) || level.getGameTime() >= bleed.until()) {
                it.remove();
                continue;
            }
            if ((level.getGameTime() - bleed.until()) % 20 != 0) continue;
            ServerPlayer source = bleed.source();
            if (source.isRemoved()) {
                it.remove();
                continue;
            }
            AbilityDamage.hurt(source, target, bleed.perSecond(), DamageStyle.SLASH);
            level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(), 3, 0.2, 0.2, 0.2, 0.05);
        }
    }

    // Starts (or refreshes) a bleed: `total` damage spread over `duration` ticks. Bosses bleed too.
    public static void bleed(LivingEntity target, ServerPlayer source, float total, int duration) {
        if (!(target.level() instanceof ServerLevel level) || total <= 0) return;
        int seconds = Math.max(1, duration / 20);
        BLEEDS.put(target, new Bleed(source, total / seconds, level.getGameTime() + seconds * 20L));
        target.addEffect(new MobEffectInstance(ModEffects.BLEEDING, seconds * 20, 0), source);
    }

    // Stunned mobs stand still and don't attack; Deadeye's straight-flying arrows drop again after 3 s
    @SubscribeEvent
    static void onEntityTick(EntityTickEvent.Pre event) {
        if (event.getEntity() instanceof AbstractArrow arrow && arrow.isNoGravity() && arrow.tickCount > 60 && !arrow.level().isClientSide()
                && arrow.getPersistentData().contains(SHOT_TAG)) {
            arrow.setNoGravity(false);
        }
        if (event.getEntity() instanceof Mob mob && !mob.level().isClientSide() && mob.hasEffect(ModEffects.STUNNED)) {
            mob.getNavigation().stop();
            mob.setTarget(null);
        }
    }

    // --- Swings: remember the attack strength before vanilla resets it ---

    @SubscribeEvent(priority = EventPriority.LOWEST)
    static void onAttack(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) AbilityRuntime.of(player).pendingStrength = player.getAttackStrengthScale(0.5f);
    }

    // --- Damage modifiers ---

    @SubscribeEvent(priority = EventPriority.LOW)
    static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (!(victim.level() instanceof ServerLevel level)) return;
        DamageSource source = event.getSource();
        Entity attacker = source.getEntity();
        Entity direct = source.getDirectEntity();
        long now = level.getGameTime();

        // A stunned mob's swings do nothing
        if (attacker instanceof Mob mob && direct == mob && mob.hasEffect(ModEffects.STUNNED)) {
            event.setCanceled(true);
            return;
        }

        // Riposte: the first entity hit inside the parry window is negated and readies the counter
        if (victim instanceof ServerPlayer player && attacker != null && attacker != player) {
            AbilityRuntime runtime = AbilityRuntime.of(player);
            if (now < runtime.parryUntil && (direct == attacker || direct instanceof Projectile)) {
                event.setCanceled(true);
                runtime.parryUntil = 0;
                runtime.counterUntil = now + runtime.counterWindow;
                player.removeEffect(ModEffects.PARRYING);
                player.addEffect(new MobEffectInstance(ModEffects.RIPOSTE, runtime.counterWindow, 0, false, true, true));
                level.sendParticles(ParticleTypes.CRIT, player.getX(), player.getY() + 1.2, player.getZ(), 16, 0.5, 0.5, 0.5, 0.3);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SHIELD_BLOCK.value(), SoundSource.PLAYERS, 1f, 1.5f);
                return;
            }
        }

        double multiplier = 1.0;
        DamageStyle style = CombatDamage.currentStyle(source);
        boolean melee = style != null && !style.isRanged();

        // Defender buffs
        if (victim.hasEffect(ModEffects.BRACED)) multiplier *= 1 - EffectTuning.get(ModEffects.BRACED, "damage_taken", 0.40);
        if (victim.hasEffect(ModEffects.BATTLE_TRANCE)) multiplier *= 1 + EffectTuning.get(ModEffects.BATTLE_TRANCE, "damage_taken", 0.10);
        if (victim.hasEffect(ModEffects.BULWARK) && melee && attacker instanceof LivingEntity hitter && direct == attacker && attacker != victim
                && (!(hitter instanceof Player) || Config.ABILITIES_AFFECT_PLAYERS.get())) {
            float reflected = (float) (event.getAmount() * EffectTuning.get(ModEffects.BULWARK, "reflect", 0.20));
            if (reflected > 0) hitter.hurtServer(level, level.damageSources().thorns(victim), reflected);
        }

        // Attacker buffs
        if (attacker instanceof ServerPlayer player && attacker != victim) {
            if (melee && player.hasEffect(ModEffects.BATTLE_TRANCE)) multiplier *= 1 + EffectTuning.get(ModEffects.BATTLE_TRANCE, "damage_dealt", 0.25);
            AbilityRuntime runtime = AbilityRuntime.of(player);
            if (melee && now < runtime.counterUntil) {
                multiplier *= 1 + runtime.counterBonus;
                runtime.counterUntil = 0;
                player.removeEffect(ModEffects.RIPOSTE);
                AbilityService.gainMomentum(player, runtime.counterMomentum);
                level.sendParticles(ParticleTypes.ENCHANTED_HIT, victim.getX(), victim.getY() + victim.getBbHeight() * 0.6, victim.getZ(), 14, 0.4, 0.4, 0.4, 0.3);
            }
        }

        // Projectiles carrying an ability bonus
        if (direct instanceof Projectile projectile) {
            CompoundTag shot = shotTag(projectile);
            if (shot != null) {
                multiplier *= shot.getFloatOr("damage", 1f);
                int root = shot.getIntOr("root", 0);
                if (root > 0 && attacker instanceof Player caster && CrowdControl.canAffect(caster, victim)) {
                    CrowdControl.apply(victim, ModEffects.ROOTED, root, 0, caster);
                }
            }
        }
        if (multiplier != 1.0) event.setAmount((float) (event.getAmount() * multiplier));
    }

    // Last Stand: no single hit takes more than a share of max health (after armour)
    @SubscribeEvent(priority = EventPriority.LOWEST)
    static void onDamagePre(LivingDamageEvent.Pre event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide() || !victim.hasEffect(ModEffects.LAST_STAND)) return;
        float cap = (float) (victim.getMaxHealth() * EffectTuning.get(ModEffects.LAST_STAND, "max_hit", 0.20));
        if (event.getNewDamage() > cap) event.setNewDamage(cap);
    }

    @SubscribeEvent
    static void onKnockback(LivingKnockBackEvent event) {
        if (event.getEntity().hasEffect(ModEffects.LAST_STAND)) event.setCanceled(true);
    }

    // --- Momentum and combat state, from damage that landed ---

    @SubscribeEvent
    static void onDamagePost(LivingDamageEvent.Post event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide()) return;
        DamageSource source = event.getSource();
        Entity attacker = source.getEntity();
        boolean realTarget = victim instanceof Mob || victim instanceof Player;

        if (attacker instanceof ServerPlayer player && attacker != victim && realTarget) {
            AbilityService.markCombat(player);
            AbilityRuntime runtime = AbilityRuntime.of(player);
            if (!AbilityDamage.active()) {
                Entity direct = source.getDirectEntity();
                if (direct == player && source.is(DamageTypes.PLAYER_ATTACK)) {
                    float strength = runtime.pendingStrength < 0 ? 1f : runtime.pendingStrength;
                    AbilityService.gainMomentum(player, Config.MOMENTUM_PER_MELEE_HIT.get() * strength * strength);
                } else if (direct instanceof Projectile projectile && !isAbilityProjectile(projectile)) {
                    AbilityService.gainMomentum(player, Config.MOMENTUM_PER_PROJECTILE_HIT.get());
                }
            }
            runtime.pendingStrength = -1;
        }
        if (victim instanceof ServerPlayer player && attacker != null && attacker != player) {
            AbilityService.markCombat(player);
            AbilityRuntime runtime = AbilityRuntime.of(player);
            long now = player.level().getGameTime();
            if (now - runtime.lastHitTakenMomentum >= 10) {
                runtime.lastHitTakenMomentum = now;
                AbilityService.gainMomentum(player, Config.MOMENTUM_PER_HIT_TAKEN.get());
            }
        }
    }

    // --- Projectiles: next-shot bonuses, Deadeye, piercing ---

    public static void markAbilityProjectile(Projectile projectile) {
        projectile.getPersistentData().putBoolean(ABILITY_PROJECTILE, true);
    }

    public static boolean isAbilityProjectile(Projectile projectile) {
        return projectile.getPersistentData().getBooleanOr(ABILITY_PROJECTILE, false);
    }

    private static CompoundTag shotTag(Projectile projectile) {
        CompoundTag data = projectile.getPersistentData();
        return data.contains(SHOT_TAG) ? data.getCompoundOrEmpty(SHOT_TAG) : null;
    }

    // Quick Draw: a bow shot counts as fully drawn
    @SubscribeEvent
    static void onArrowLoose(ArrowLooseEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        AbilityRuntime runtime = AbilityRuntime.of(player);
        if (runtime.nextShot != null && runtime.nextShot.instantCharge() && player.level().getGameTime() < runtime.nextShotUntil) {
            event.setCharge(Math.max(event.getCharge(), 20));
        }
    }

    @SubscribeEvent
    static void onProjectileJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || event.loadedFromDisk() || !(event.getEntity() instanceof Projectile projectile)) return;
        // A stunned mob can't shoot either
        if (projectile.getOwner() instanceof Mob mob && mob.hasEffect(ModEffects.STUNNED)) {
            event.setCanceled(true);
            return;
        }
        // Only arrows and tridents carry next-shot bonuses (not snowballs, pearls or fishing hooks)
        if (!(projectile instanceof AbstractArrow) || !(projectile.getOwner() instanceof ServerPlayer player) || isAbilityProjectile(projectile)) return;
        AbilityRuntime runtime = AbilityRuntime.of(player);
        float damage = 1f;
        int root = 0, pierce = 0;
        long now = player.level().getGameTime();
        if (runtime.nextShot != null && now < runtime.nextShotUntil) {
            damage *= runtime.nextShot.damage();
            root = runtime.nextShot.root();
            pierce = runtime.nextShot.pierce();
            runtime.nextShot = null;
        }
        if (player.hasEffect(ModEffects.DEADEYE) && projectile instanceof AbstractArrow) {
            projectile.setNoGravity(true);
            damage *= 1 + (float) EffectTuning.get(ModEffects.DEADEYE, "damage", 0.30);
            if (projectile instanceof SkillArrow arrow && arrow.weight() == SkillArrow.Weight.HEAVY) {
                pierce += (int) EffectTuning.get(ModEffects.DEADEYE, "pierce", 1);
            }
        }
        if (damage == 1f && root == 0 && pierce == 0) return;
        CompoundTag shot = new CompoundTag();
        shot.putFloat("damage", damage);
        shot.putInt("root", root);
        shot.putInt("pierce", pierce);
        shot.putFloat("base", projectile instanceof SkillArrow arrow ? (float) arrow.weight().baseDamage() : 2f);
        projectile.getPersistentData().put(SHOT_TAG, shot);
    }

    // Piercing: while a shot has pierces left, it damages what it hits and keeps flying
    @SubscribeEvent
    static void onProjectileImpact(ProjectileImpactEvent event) {
        Projectile projectile = event.getProjectile();
        if (projectile.level().isClientSide() || !(event.getRayTraceResult() instanceof EntityHitResult hit)) return;
        CompoundTag shot = shotTag(projectile);
        if (shot == null || !(projectile.getOwner() instanceof ServerPlayer player) || !(hit.getEntity() instanceof LivingEntity target)) return;
        Set<Integer> pierced = PIERCED.computeIfAbsent(projectile.getUUID(), id -> new HashSet<>());
        if (pierced.contains(target.getId())) {
            event.setCanceled(true);
            return;
        }
        int left = shot.getIntOr("pierce", 0);
        if (left <= 0) {
            PIERCED.remove(projectile.getUUID());
            return;
        }
        pierced.add(target.getId());
        shot.putInt("pierce", left - 1);
        float amount = (float) Math.ceil(projectile.getDeltaMovement().length() * shot.getFloatOr("base", 2f)) * shot.getFloatOr("damage", 1f);
        DamageStyle style = projectile instanceof SkillArrow arrow ? arrow.weight().style() : DamageStyle.MEDIUM;
        AbilityDamage.hurt(player, target, amount, style);
        event.setCanceled(true);
        if (PIERCED.size() > 256) PIERCED.clear(); // arrows that never landed
    }

    // --- Unshackled: immune to slows, roots and stuns ---

    @SubscribeEvent
    static void onEffectApplicable(MobEffectEvent.Applicable event) {
        if (event.getEntity().hasEffect(ModEffects.UNSHACKLED) && CrowdControl.isControlEffect(event.getEffectInstance().getEffect())) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }
}
