package com.toddlandis.skillcraft.combat;

import java.util.HashMap;
import java.util.Map;

import com.toddlandis.skillcraft.SkillCraft;
import com.toddlandis.skillcraft.registry.ModAttachments;
import com.toddlandis.skillcraft.skill.Skill;
import com.toddlandis.skillcraft.skill.SkillManager;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

// OSRS-style combat XP, paid per point of health actually removed from a mob:
//   4 XP per damage point to Melee/Ranged and/or Defence (split by attack style)
//   + 1.33 XP per damage point to Constitution
// Anti-farming: no XP for players, armour stands, babies, tamed animals or creative players;
// mobs from mob spawners give half. See docs/COMBAT.md.
@EventBusSubscriber(modid = SkillCraft.MODID)
public final class CombatXp {
    public static final double XP_PER_DAMAGE = 4.0;
    public static final double CONSTITUTION_XP_PER_DAMAGE = 4.0 / 3.0;
    public static final double SPAWNER_FACTOR = 0.5;
    public static final String SPAWNER_TAG = "skillcraft.spawner_mob";

    // Health of each entity just before damage lands, so overkill pays nothing
    private static final Map<Integer, Float> HEALTH_BEFORE = new HashMap<>();

    private CombatXp() {}

    @SubscribeEvent
    static void onSpawn(FinalizeSpawnEvent event) {
        if (event.getSpawnType() == EntitySpawnReason.SPAWNER) event.getEntity().addTag(SPAWNER_TAG);
    }

    @SubscribeEvent
    static void onDamagePre(LivingDamageEvent.Pre event) {
        if (!event.getEntity().level().isClientSide()) HEALTH_BEFORE.put(event.getEntity().getId(), event.getEntity().getHealth());
    }

    @SubscribeEvent
    static void onDamagePost(LivingDamageEvent.Post event) {
        LivingEntity target = event.getEntity();
        Float before = HEALTH_BEFORE.remove(target.getId());
        if (target.level().isClientSide()) return;
        ServerPlayer player = creditedPlayer(event.getSource());
        if (player == null || !givesXp(target)) return;
        double dealt = Math.min(event.getHealthDamage(), before != null ? before : event.getHealthDamage());
        if (dealt <= 0) return;

        if (target instanceof PlayerDamageTracker tracker) tracker.recordPlayerDamage(player, dealt);
        DamageStyle style = DamageStyles.of(event.getSource());
        award(player, dealt, style != null && style.isRanged(), target.entityTags().contains(SPAWNER_TAG) ? SPAWNER_FACTOR : 1.0);
    }

    public static void award(ServerPlayer player, double damage, boolean ranged, double factor) {
        AttackStyle style = player.getData(ModAttachments.ATTACK_STYLE);
        double xp = damage * XP_PER_DAMAGE * factor;
        if (style.offenceShare() > 0) SkillManager.awardXp(player, ranged ? Skill.RANGED : Skill.MELEE, xp * style.offenceShare());
        if (style.defenceShare() > 0) SkillManager.awardXp(player, Skill.DEFENCE, xp * style.defenceShare());
        SkillManager.awardXp(player, Skill.CONSTITUTION, damage * CONSTITUTION_XP_PER_DAMAGE * factor);
    }

    // The player who gets credit for this damage (the shooter for projectiles), or null
    public static ServerPlayer creditedPlayer(DamageSource source) {
        if (source.getEntity() instanceof ServerPlayer player && !player.isCreative() && !player.isSpectator()) return player;
        return null;
    }

    public static boolean givesXp(LivingEntity target) {
        if (!(target instanceof Mob mob)) return false; // players, armour stands
        if (mob.isBaby()) return false;
        if (mob instanceof TamableAnimal tamable && tamable.isTame()) return false;
        if (mob instanceof AbstractHorse horse && horse.isTamed()) return false;
        return true;
    }
}
