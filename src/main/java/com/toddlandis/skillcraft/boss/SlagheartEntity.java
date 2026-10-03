package com.toddlandis.skillcraft.boss;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.toddlandis.skillcraft.arena.ArenaInstance;
import com.toddlandis.skillcraft.arena.ArenaManager;
import com.toddlandis.skillcraft.combat.CombatDamage;
import com.toddlandis.skillcraft.combat.DamageStyle;
import com.toddlandis.skillcraft.combat.PlayerDamageTracker;
import com.toddlandis.skillcraft.combat.StyleVulnerable;
import com.toddlandis.skillcraft.registry.ModDataMaps;
import com.toddlandis.skillcraft.registry.ModSounds;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.cubemob.MagmaCube;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

// Slagheart, the Forge Warden: the first SkillCraft boss. See docs/BOSS_SLAGHEART.md.
//
// The fight is a server-side state machine. Each attack is an "action" with a windup (animation,
// sound and particles that telegraph where it will land) and a strike. The current action is
// synced so clients play the matching keyframe animation. Health thresholds change phase, which
// changes the attack rotation; the boss vents after each rotation (a vulnerability window).
// Balance values come from the boss_tuning data map.
public class SlagheartEntity extends Monster implements StyleVulnerable, PlayerDamageTracker, com.toddlandis.skillcraft.combat.BossCrowdControl {
    private static final EntityDataAccessor<Integer> DATA_ACTION = SynchedEntityData.defineId(SlagheartEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_PHASE = SynchedEntityData.defineId(SlagheartEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_SPEED = SynchedEntityData.defineId(SlagheartEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_STOKED = SynchedEntityData.defineId(SlagheartEntity.class, EntityDataSerializers.BOOLEAN);
    // Bumped whenever an animation (re)starts, so the same action can be replayed (double slam)
    private static final EntityDataAccessor<Integer> DATA_SERIAL = SynchedEntityData.defineId(SlagheartEntity.class, EntityDataSerializers.INT);

    public static final String MITE_TAG = "skillcraft.cinder_mite";
    public static final double PHASE_2_AT = 0.65, PHASE_3_AT = 0.30;
    private static final double HAMMER_RADIUS = 3.0, HAMMER_REACH = 5.0;
    private static final double SWEEP_RADIUS = 5.0, SWEEP_HALF_ANGLE = 75;
    private static final double RING_SPEED = 0.4, RING_MAX = 17;
    private static final int DEATH_TICKS = 60;

    // Client-side animation states, one per action (see SlagheartModel)
    public final Map<SlagheartAction, AnimationState> animations = new HashMap<>();
    public final AnimationState idleAnimation = new AnimationState();

    private final ServerBossEvent bossEvent = new ServerBossEvent(UUID.randomUUID(), Component.translatable("entity.skillcraft.slagheart"),
            BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);

    // --- server state ---
    private int instanceId = -1;
    private Vec3 home;
    private int floorY = Integer.MIN_VALUE;
    private float actionTime;
    private float timeScale = 1f;
    private int rotationIndex;
    private int cooldown = 20;
    // Daze from stun abilities (spec 2.6): delays the next attack, never cuts a windup short
    private long lastDazeTick = Long.MIN_VALUE / 2;
    private int pendingDaze;
    private int fightTicks;
    private int enrageStacks;
    private boolean hammerSecondSlam;
    private Vec3 aimPos = Vec3.ZERO;
    private float lockedYaw;
    private final Map<UUID, Double> damageByPlayer = new HashMap<>();
    private final List<UUID> mites = new ArrayList<>();
    private final List<Hazard> hazards = new ArrayList<>();
    private final Map<BlockPos, BlockState> replacedBlocks = new HashMap<>();
    private final List<Ring> rings = new ArrayList<>();

    // A floor cell that turns to magma, then (for ember cells) erupts
    private static final class Hazard {
        final List<BlockPos> cells;
        final int eruptAt, endAt;
        final boolean erupts;
        boolean erupted;

        Hazard(List<BlockPos> cells, int eruptAt, int endAt, boolean erupts) {
            this.cells = cells;
            this.eruptAt = eruptAt;
            this.endAt = endAt;
            this.erupts = erupts;
        }
    }

    // An expanding Forge Quake shockwave
    private static final class Ring {
        final Vec3 center;
        final int startTick;
        final Set<UUID> hit = new HashSet<>();

        Ring(Vec3 center, int startTick) {
            this.center = center;
            this.startTick = startTick;
        }
    }

    public SlagheartEntity(EntityType<? extends SlagheartEntity> type, Level level) {
        super(type, level);
        for (SlagheartAction action : SlagheartAction.values()) animations.put(action, new AnimationState());
        this.xpReward = 0;
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 500)
                .add(Attributes.ARMOR, 8)
                .add(Attributes.MOVEMENT_SPEED, 0.24)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 40)
                .add(Attributes.ATTACK_DAMAGE, 10)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ACTION, SlagheartAction.NONE.ordinal());
        builder.define(DATA_PHASE, 1);
        builder.define(DATA_SPEED, 1f);
        builder.define(DATA_STOKED, false);
        builder.define(DATA_SERIAL, 0);
    }

    // --- setup (called by the arena when an instance starts, or by /summon for testing) ---

    // Ties this boss to an arena instance and scales it for the party size
    public void setup(int instanceId, Vec3 home, int partySize) {
        this.instanceId = instanceId;
        this.home = home;
        this.floorY = BlockPos.containing(home).getY() - 1;
        BossTuning tuning = tuning();
        double health = tuning.health() * (1 + tuning.partyHealthScale() * Math.max(0, partySize - 1));
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
        setHealth((float) health);
        startAction(SlagheartAction.AWAKEN, 1f);
    }

    public BossTuning tuning() {
        BossTuning tuning = BuiltInRegistries.ENTITY_TYPE.getResourceKey(getType())
                .map(key -> BuiltInRegistries.ENTITY_TYPE.getData(ModDataMaps.BOSS_TUNING, key)).orElse(null);
        return tuning != null ? tuning : BossDefaults.SLAGHEART;
    }

    public int instanceId() {
        return instanceId;
    }

    public int phase() {
        return entityData.get(DATA_PHASE);
    }

    public SlagheartAction action() {
        return SlagheartAction.byId(entityData.get(DATA_ACTION));
    }

    public float animationSpeed() {
        return entityData.get(DATA_SPEED);
    }

    public boolean isVented() {
        return action() == SlagheartAction.VENT;
    }

    public boolean isStoked() {
        return entityData.get(DATA_STOKED);
    }

    public Map<UUID, Double> damageByPlayer() {
        return damageByPlayer;
    }

    // --- damage ---

    @Override
    public double damageModifier(DamageStyle style) {
        return tuning().stanceModifier(isVented() ? "vented" : "banked", style);
    }

    @Override
    public void recordPlayerDamage(ServerPlayer player, double damage) {
        damageByPlayer.merge(player.getUUID(), damage, Double::sum);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        SlagheartAction action = action();
        if (action == SlagheartAction.AWAKEN || action == SlagheartAction.ROAR || action == SlagheartAction.DEATH) return false;
        if (source.getEntity() instanceof SlagheartEntity || source.getEntity() instanceof MagmaCube) return false;
        BossTuning tuning = tuning();
        if (isVented()) amount *= (float) tuning.ventMultiplier();
        if (isStoked()) amount *= (float) tuning.stokedMultiplier();
        boolean hurt = super.hurtServer(level, source, amount);
        if (hurt) checkPhase(level);
        return hurt;
    }

    private void checkPhase(ServerLevel level) {
        float fraction = getHealth() / getMaxHealth();
        int phase = phase();
        if (phase == 1 && fraction <= PHASE_2_AT || phase == 2 && fraction <= PHASE_3_AT) {
            entityData.set(DATA_PHASE, phase + 1);
            rotationIndex = 0;
            clearAttackState();
            startAction(SlagheartAction.ROAR, 1f);
            announce(level, Component.translatable(phase == 1 ? "message.skillcraft.slagheart_phase2" : "message.skillcraft.slagheart_phase3")
                    .withStyle(ChatFormatting.GOLD));
        }
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean causeFallDamage(double distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean canUsePortal(boolean ignorePassenger) {
        return false;
    }

    @Override
    public boolean addEffect(MobEffectInstance effect, Entity source) {
        // Immune to crowd control; only Sundered (a damage debuff) sticks
        return effect.is(com.toddlandis.skillcraft.registry.ModEffects.SUNDERED) && super.addEffect(effect, source);
    }

    // --- main loop ---

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (home == null) {
            home = position();
            floorY = blockPosition().getY() - 1;
        }
        ArenaInstance instance = instanceId >= 0 ? ArenaManager.get(level.getServer()).instance(instanceId) : null;
        if (instanceId >= 0 && (instance == null || !getUUID().equals(instance.boss()))) {
            // Not the arena's current boss (left over from before a restart or a reset)
            restoreBlocks(level);
            discard();
            return;
        }
        // Leashed to the arena floor
        if (instance != null && home != null && action() != SlagheartAction.DEATH) {
            double dx = getX() - (instance.center().getX() + 0.5), dz = getZ() - (instance.center().getZ() + 0.5);
            double leash = ArenaInstance.FIGHT_RADIUS - 1;
            if (dx * dx + dz * dz > leash * leash) teleportTo(home.x, home.y, home.z);
        }
        tickMites(level);
        tickHazards(level);
        tickRings(level);
        updateBossBar();

        SlagheartAction action = action();
        if (action != SlagheartAction.AWAKEN && action != SlagheartAction.DEATH) {
            fightTicks++;
            tickEnrage(level);
        }
        LivingEntity target = findTarget(level);
        setTarget(target);

        if (action != SlagheartAction.NONE) {
            float before = actionTime;
            actionTime += timeScale;
            tickAction(level, action, before, actionTime, target);
            return;
        }
        if (target == null) {
            getNavigation().stop();
            return;
        }
        getLookControl().setLookAt(target, 30, 30);
        if (distanceTo(target) > 4) getNavigation().moveTo(target, 1.0);
        else getNavigation().stop();
        if (--cooldown <= 0) startNextAttack(target);
    }

    private LivingEntity findTarget(ServerLevel level) {
        Player best = null;
        double bestDistance = Double.MAX_VALUE;
        for (ServerPlayer player : fightPlayers(level)) {
            double distance = distanceToSqr(player);
            if (distance < bestDistance) {
                best = player;
                bestDistance = distance;
            }
        }
        return best;
    }

    // Players taking part: everyone on the arena floor, or (outside an arena) anyone close by
    public List<ServerPlayer> fightPlayers(ServerLevel level) {
        List<ServerPlayer> players = new ArrayList<>();
        ArenaInstance instance = instanceId >= 0 ? ArenaManager.get(level.getServer()).instance(instanceId) : null;
        for (ServerPlayer player : level.players()) {
            if (!player.isAlive() || player.isSpectator() || player.isCreative()) continue;
            if (instance != null ? !instance.isFighting(player) : player.distanceToSqr(this) > 32 * 32) continue;
            players.add(player);
        }
        return players;
    }

    // --- attack selection ---

    private static final SlagheartAction[][] ROTATIONS = {
            {SlagheartAction.HAMMERFALL, null, SlagheartAction.HAMMERFALL, SlagheartAction.SLAG_SPIT, SlagheartAction.VENT},
            {SlagheartAction.HAMMERFALL, SlagheartAction.EMBER_RAIN, null, SlagheartAction.HAMMERFALL, SlagheartAction.SLAG_SPIT, SlagheartAction.VENT},
            {SlagheartAction.FORGE_QUAKE, SlagheartAction.HAMMERFALL, SlagheartAction.EMBER_RAIN, SlagheartAction.SLAG_SPIT, null, SlagheartAction.VENT},
    };

    private void startNextAttack(LivingEntity target) {
        SlagheartAction[] rotation = ROTATIONS[phase() - 1];
        SlagheartAction next = rotation[rotationIndex % rotation.length];
        rotationIndex++;
        // null = "sweep if they're close, otherwise spit"
        if (next == null) next = distanceTo(target) <= SWEEP_RADIUS ? SlagheartAction.TONG_SWEEP : SlagheartAction.SLAG_SPIT;
        // Hammerfall needs the boss within reach; spit instead if the target is far away
        if (next == SlagheartAction.HAMMERFALL && distanceTo(target) > 10) next = SlagheartAction.SLAG_SPIT;
        float speed = windupSpeed();
        if (next == SlagheartAction.VENT) speed = 60f / tuning().ventTicks(phase());
        startAction(next, speed);
        aimAt(target, next);
    }

    // Phase 3 winds up 25% faster; each enrage stack another 10%
    private float windupSpeed() {
        float speed = phase() >= 3 ? 1.333f : 1f;
        return speed * (1 + 0.1f * enrageStacks);
    }

    private void aimAt(LivingEntity target, SlagheartAction action) {
        lookAt(target, 360, 30);
        lockedYaw = getYRot();
        setYBodyRot(lockedYaw);
        Vec3 toTarget = target.position().subtract(position());
        double distance = Math.min(HAMMER_REACH, toTarget.horizontalDistance());
        Vec3 flat = new Vec3(toTarget.x, 0, toTarget.z).normalize();
        aimPos = action == SlagheartAction.HAMMERFALL ? position().add(flat.scale(distance)) : target.position();
        hammerSecondSlam = false;
    }

    // speed > 1 makes the whole action (windup, strike, animation) faster
    private void startAction(SlagheartAction action, float speed) {
        actionTime = 0;
        timeScale = speed;
        lockedYaw = getYRot();
        entityData.set(DATA_ACTION, action.ordinal());
        restartAnimation(speed);
        getNavigation().stop();
        if (level() instanceof ServerLevel level) playActionSound(level, action);
    }

    private void restartAnimation(float animationSpeed) {
        entityData.set(DATA_SPEED, animationSpeed);
        entityData.set(DATA_SERIAL, entityData.get(DATA_SERIAL) + 1);
    }

    // A stun ability landed. Between attacks the next one comes later; mid-attack (windup or strike)
    // the delay is added once the attack has finished. At most once per DAZE_LOCKOUT ticks.
    @Override
    public boolean daze(int extraTicks) {
        long now = level().getGameTime();
        if (now - lastDazeTick < com.toddlandis.skillcraft.ability.CrowdControl.DAZE_LOCKOUT) return false;
        lastDazeTick = now;
        if (action() == SlagheartAction.NONE) cooldown += extraTicks;
        else pendingDaze += extraTicks;
        return true;
    }

    // Ticks until the next attack starts (for GameTests)
    public int attackCooldown() {
        return cooldown;
    }

    // Dev showcase only (dev/Showcase): plays an action's animation without its gameplay. Use with AI off.
    public void showcasePose(SlagheartAction action) {
        entityData.set(DATA_ACTION, action.ordinal());
        restartAnimation(1f);
    }

    private void endAction() {
        entityData.set(DATA_ACTION, SlagheartAction.NONE.ordinal());
        timeScale = 1f;
        restartAnimation(1f);
        cooldown = phase() >= 3 ? 10 : 15;
        cooldown += pendingDaze;
        pendingDaze = 0;
    }

    private void clearAttackState() {
        actionTime = 0;
        hammerSecondSlam = false;
    }

    private static boolean crossed(float before, float after, float at) {
        return before < at && after >= at;
    }

    // --- actions ---

    private void tickAction(ServerLevel level, SlagheartAction action, float before, float now, LivingEntity target) {
        BossTuning tuning = tuning();
        setYRot(lockedYaw);
        setYBodyRot(lockedYaw);
        switch (action) {
            case AWAKEN -> {
                if ((int) now % 4 == 0) level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1, getZ(), 6, 1, 0.5, 1, 0.02);
                if (now >= 60) endAction();
            }
            case HAMMERFALL -> {
                int windup = tuning.windup("hammerfall", 30);
                if ((int) now % 3 == 0 && now < windup) ringParticles(level, aimPos, HAMMER_RADIUS, ParticleTypes.FLAME);
                if (crossed(before, now, windup)) hammerSlam(level, tuning.damage("hammerfall", 14));
                // Phase 3: a second, quicker slam where the target is standing now
                if (phase() >= 3 && !hammerSecondSlam && crossed(before, now, windup + 2)) {
                    if (target != null) aimAt(target, SlagheartAction.HAMMERFALL);
                    hammerSecondSlam = true;
                    restartAnimation(timeScale * 2);
                }
                if (hammerSecondSlam) {
                    float second = windup + 2 + windup / 2f;
                    if ((int) now % 3 == 0 && now < second) ringParticles(level, aimPos, HAMMER_RADIUS, ParticleTypes.SOUL_FIRE_FLAME);
                    if (crossed(before, now, second)) hammerSlam(level, tuning.damage("hammerfall", 14));
                    if (now >= second + 16) endAction();
                } else if (now >= windup + 20 && phase() < 3) {
                    endAction();
                }
            }
            case TONG_SWEEP -> {
                int windup = tuning.windup("tong_sweep", 18);
                if ((int) now % 3 == 0 && now < windup) arcParticles(level);
                if (crossed(before, now, windup)) tongSweep(level, tuning.damage("tong_sweep", 10));
                if (now >= windup + 18) endAction();
            }
            case SLAG_SPIT -> {
                int windup = tuning.windup("slag_spit", 20);
                if ((int) now % 2 == 0 && now < windup) {
                    level.sendParticles(ParticleTypes.LAVA, getX(), getY() + 2.4, getZ(), 2, 0.4, 0.3, 0.4, 0);
                    chimneySmoke(level, 3);
                }
                if (crossed(before, now, windup)) slagSpit(level, tuning.damage("slag_glob", 6));
                if (now >= windup + 14) endAction();
            }
            case EMBER_RAIN -> {
                int windup = tuning.windup("ember_rain", 25);
                if (crossed(before, now, windup)) emberRain(level);
                if (now >= windup + 20) endAction();
            }
            case FORGE_QUAKE -> {
                int windup = tuning.windup("forge_quake", 20);
                if ((int) now % 4 == 0 && now < windup) level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, getX(), floorY + 1.1, getZ(), 4, 1.5, 0, 1.5, 0.01);
                for (int i = 0; i < 3; i++) {
                    if (crossed(before, now, windup + i * 15)) {
                        rings.add(new Ring(new Vec3(getX(), floorY + 1, getZ()), fightTicks));
                        sound(level, ModSounds.SLAGHEART_QUAKE.get(), 2f, 0.8f + i * 0.1f);
                    }
                }
                if (now >= windup + 40) endAction();
            }
            case VENT -> {
                if ((int) now % 3 == 0) {
                    level.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 2.2, getZ(), 3, 0.5, 0.4, 0.5, 0.03);
                    chimneySmoke(level, 2);
                }
                if (now >= 60) endAction();
            }
            case ROAR -> {
                if (crossed(before, now, 12)) heatPulse(level, tuning.damage("heat_pulse", 4));
                if (phase() >= 3 && crossed(before, now, 6) && home != null) {
                    // Leap back to the centre of the arena
                    level.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 1, getZ(), 2, 0.5, 0.5, 0.5, 0);
                    teleportTo(home.x, home.y, home.z);
                    level.sendParticles(ParticleTypes.EXPLOSION, home.x, home.y + 1, home.z, 3, 1, 0.5, 1, 0);
                }
                if (crossed(before, now, 40)) spawnMites(level);
                if (now >= 50) endAction();
            }
            default -> endAction();
        }
    }

    private void hammerSlam(ServerLevel level, float damage) {
        sound(level, ModSounds.SLAGHEART_HAMMER_SLAM.get(), 2.5f, 0.8f);
        level.sendParticles(ParticleTypes.EXPLOSION, aimPos.x, floorY + 1.2, aimPos.z, 2, 0.6, 0.1, 0.6, 0);
        level.sendParticles(ParticleTypes.LAVA, aimPos.x, floorY + 1.2, aimPos.z, 16, 1.5, 0.2, 1.5, 0.2);
        BlockState floor = level.getBlockState(BlockPos.containing(aimPos.x, floorY, aimPos.z));
        if (!floor.isAir()) level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, floor), aimPos.x, floorY + 1.1, aimPos.z, 40, 1.5, 0.1, 1.5, 0.15);
        for (ServerPlayer player : fightPlayers(level)) {
            Vec3 p = player.position();
            if (Math.hypot(p.x - aimPos.x, p.z - aimPos.z) <= HAMMER_RADIUS && Math.abs(p.y - (floorY + 1)) < 2.5) {
                if (CombatDamage.hurtWithStyle(level, player, damageSources().mobAttack(this), scaled(damage), DamageStyle.CRUSH)) {
                    player.push(0, 0.6, 0);
                    player.hurtMarked = true;
                    player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 20, 1));
                }
            }
        }
    }

    private void tongSweep(ServerLevel level, float damage) {
        sound(level, ModSounds.SLAGHEART_SWEEP.get(), 2f, 1f);
        Vec3 facing = Vec3.directionFromRotation(0, lockedYaw);
        for (int i = -4; i <= 4; i++) {
            Vec3 dir = Vec3.directionFromRotation(0, lockedYaw + i * 18);
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, getX() + dir.x * 3, getY() + 1.4, getZ() + dir.z * 3, 1, 0, 0, 0, 0);
        }
        for (ServerPlayer player : fightPlayers(level)) {
            Vec3 to = player.position().subtract(position());
            double distance = to.horizontalDistance();
            if (distance > SWEEP_RADIUS || distance < 0.01) continue;
            double angle = Math.toDegrees(Math.acos(Mth.clamp((to.x * facing.x + to.z * facing.z) / distance, -1, 1)));
            if (angle > SWEEP_HALF_ANGLE) continue;
            if (CombatDamage.hurtWithStyle(level, player, damageSources().mobAttack(this), scaled(damage), DamageStyle.SLASH)) {
                shove(player, getX(), getZ(), 1.2, 0.3);
                player.hurtMarked = true;
            }
        }
    }

    private void slagSpit(ServerLevel level, float damage) {
        sound(level, ModSounds.SLAGHEART_SPIT.get(), 2f, 0.9f);
        for (ServerPlayer player : fightPlayers(level)) {
            if (player.distanceToSqr(this) > 28 * 28) continue;
            for (int i = 0; i < 3; i++) {
                SlagGlob glob = new SlagGlob(level, this, scaled(damage));
                glob.setPos(getX(), getY() + 2.6, getZ());
                double dx = player.getX() - getX(), dz = player.getZ() - getZ();
                double dy = player.getY() - glob.getY();
                double horizontal = Math.sqrt(dx * dx + dz * dz);
                glob.shoot(dx, dy + horizontal * 0.25, dz, 0.9f + 0.02f * (float) horizontal, 5f + i * 4f);
                level.addFreshEntity(glob);
            }
        }
    }

    private void emberRain(ServerLevel level) {
        sound(level, ModSounds.SLAGHEART_EMBER.get(), 2f, 1f);
        List<ServerPlayer> players = fightPlayers(level);
        int count = 6 + 2 * Math.max(0, players.size() - 1);
        List<BlockPos> centres = new ArrayList<>();
        for (ServerPlayer player : players) centres.add(BlockPos.containing(player.getX(), floorY, player.getZ()));
        Vec3 centre = home != null ? home : position();
        for (int i = 0; i < count * 4 && centres.size() < count + players.size(); i++) {
            double angle = getRandom().nextDouble() * Math.PI * 2, radius = 2 + getRandom().nextDouble() * 11;
            centres.add(BlockPos.containing(centre.x + Math.cos(angle) * radius, floorY, centre.z + Math.sin(angle) * radius));
        }
        for (BlockPos c : centres) {
            List<BlockPos> cells = new ArrayList<>();
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) cells.add(c.offset(dx, 0, dz));
            addHazard(level, cells, 30, 70, true);
        }
    }

    // Slag Spit puddles: a single magma cell for 4 seconds
    public void addSlagPuddle(BlockPos pos) {
        if (level() instanceof ServerLevel level && floorY != Integer.MIN_VALUE) {
            addHazard(level, List.of(new BlockPos(pos.getX(), floorY, pos.getZ())), 0, 80, false);
        }
    }

    private void addHazard(ServerLevel level, List<BlockPos> cells, int eruptIn, int lastsFor, boolean erupts) {
        List<BlockPos> placed = new ArrayList<>();
        for (BlockPos cell : cells) {
            BlockState state = level.getBlockState(cell);
            if (state.isAir() || !level.getBlockState(cell.above()).isAir()) continue; // only open floor
            if (!replacedBlocks.containsKey(cell)) replacedBlocks.put(cell.immutable(), state);
            level.setBlock(cell, Blocks.MAGMA_BLOCK.defaultBlockState(), 2);
            placed.add(cell.immutable());
        }
        if (!placed.isEmpty()) hazards.add(new Hazard(placed, fightTicks + eruptIn, fightTicks + lastsFor, erupts));
    }

    private void tickHazards(ServerLevel level) {
        for (var it = hazards.iterator(); it.hasNext(); ) {
            Hazard hazard = it.next();
            if (hazard.erupts && !hazard.erupted && fightTicks < hazard.eruptAt && fightTicks % 4 == 0) {
                for (BlockPos cell : hazard.cells) level.sendParticles(ParticleTypes.SMOKE, cell.getX() + 0.5, cell.getY() + 1.1, cell.getZ() + 0.5, 1, 0.2, 0, 0.2, 0.01);
            }
            if (hazard.erupts && !hazard.erupted && fightTicks >= hazard.eruptAt) {
                hazard.erupted = true;
                erupt(level, hazard);
            }
            if (fightTicks >= hazard.endAt) {
                for (BlockPos cell : hazard.cells) restoreBlock(level, cell);
                it.remove();
            }
        }
    }

    private void erupt(ServerLevel level, Hazard hazard) {
        float damage = scaled(tuning().damage("ember_rain", 8));
        for (BlockPos cell : hazard.cells) level.sendParticles(ParticleTypes.FLAME, cell.getX() + 0.5, cell.getY() + 1.5, cell.getZ() + 0.5, 6, 0.2, 0.6, 0.2, 0.04);
        BlockPos first = hazard.cells.getFirst();
        level.playSound(null, first, ModSounds.SLAGHEART_ERUPT.get(), SoundSource.HOSTILE, 1f, 0.9f + getRandom().nextFloat() * 0.2f);
        for (ServerPlayer player : fightPlayers(level)) {
            BlockPos below = BlockPos.containing(player.getX(), floorY, player.getZ());
            if (hazard.cells.contains(below) && player.getY() - (floorY + 1) < 1.5) {
                if (player.hurtServer(level, damageSources().inFire(), damage)) player.igniteForSeconds(4);
            }
        }
    }

    private void restoreBlock(ServerLevel level, BlockPos cell) {
        BlockState original = replacedBlocks.remove(cell);
        if (original != null && level.getBlockState(cell).is(Blocks.MAGMA_BLOCK)) level.setBlock(cell, original, 2);
    }

    private void restoreBlocks(ServerLevel level) {
        for (BlockPos cell : new ArrayList<>(replacedBlocks.keySet())) restoreBlock(level, cell);
        hazards.clear();
    }

    private void tickRings(ServerLevel level) {
        float damage = scaled(tuning().damage("forge_quake", 8));
        for (var it = rings.iterator(); it.hasNext(); ) {
            Ring ring = it.next();
            double radius = (fightTicks - ring.startTick) * RING_SPEED;
            if (radius > RING_MAX) {
                it.remove();
                continue;
            }
            if (fightTicks % 2 == 0) ringParticles(level, ring.center, radius, ParticleTypes.FLAME);
            for (ServerPlayer player : fightPlayers(level)) {
                if (ring.hit.contains(player.getUUID())) continue;
                double d = Math.hypot(player.getX() - ring.center.x, player.getZ() - ring.center.z);
                if (Math.abs(d - radius) > 0.7 || !player.onGround()) continue;
                ring.hit.add(player.getUUID());
                if (CombatDamage.hurtWithStyle(level, player, damageSources().mobAttack(this), damage, DamageStyle.CRUSH)) {
                    shove(player, ring.center.x, ring.center.z, 0.8, 0.3);
                    player.hurtMarked = true;
                }
            }
        }
    }

    private void heatPulse(ServerLevel level, float damage) {
        sound(level, ModSounds.SLAGHEART_ROAR.get(), 3f, 0.8f);
        level.sendParticles(ParticleTypes.FLAME, getX(), getY() + 1.5, getZ(), 60, 3, 1, 3, 0.1);
        for (ServerPlayer player : fightPlayers(level)) {
            if (player.distanceToSqr(this) > 36) continue;
            if (player.hurtServer(level, damageSources().mobAttack(this), damage)) {
                shove(player, getX(), getZ(), 1.5, 0.4);
                player.hurtMarked = true;
            }
        }
    }

    // --- Cinder Mites (adds) ---

    private void spawnMites(ServerLevel level) {
        int count = 2 + Math.max(0, fightPlayers(level).size() - 1);
        for (int i = 0; i < count; i++) {
            MagmaCube mite = EntityTypes.MAGMA_CUBE.create(level, EntitySpawnReason.MOB_SUMMONED);
            if (mite == null) continue;
            double angle = Math.PI * 2 * i / count;
            mite.setSize(1, true);
            mite.getAttribute(Attributes.MAX_HEALTH).setBaseValue(8);
            mite.setHealth(8);
            mite.setCustomName(Component.translatable("entity.skillcraft.cinder_mite"));
            mite.addTag(MITE_TAG);
            mite.setPersistenceRequired();
            mite.setPos(getX() + Math.cos(angle) * 3, getY() + 0.5, getZ() + Math.sin(angle) * 3);
            level.addFreshEntity(mite);
            mites.add(mite.getUUID());
            level.sendParticles(ParticleTypes.LAVA, mite.getX(), mite.getY(), mite.getZ(), 6, 0.3, 0.3, 0.3, 0);
        }
        if (count > 0) announce(level, Component.translatable("message.skillcraft.slagheart_mites").withStyle(ChatFormatting.RED));
    }

    private void tickMites(ServerLevel level) {
        mites.removeIf(id -> !(level.getEntity(id) instanceof MagmaCube mite) || !mite.isAlive());
        boolean stoked = !mites.isEmpty();
        if (stoked != isStoked()) entityData.set(DATA_STOKED, stoked);
        if (stoked && tickCount % 4 == 0) level.sendParticles(ParticleTypes.FLAME, getX(), getY() + 1.5, getZ(), 3, 1, 1.2, 1, 0.01);
    }

    private void discardMites(ServerLevel level) {
        for (UUID id : mites) if (level.getEntity(id) instanceof MagmaCube mite) mite.discard();
        mites.clear();
    }

    // --- soft enrage ---

    private void tickEnrage(ServerLevel level) {
        int enrageTicks = tuning().enrageSeconds() * 20;
        if (fightTicks == enrageTicks - 60 * 20) {
            announce(level, Component.translatable("message.skillcraft.slagheart_enrage_warning").withStyle(ChatFormatting.RED));
        }
        if (fightTicks >= enrageTicks && (fightTicks - enrageTicks) % (60 * 20) == 0 && enrageStacks < 4) {
            enrageStacks++;
            announce(level, Component.translatable("message.skillcraft.slagheart_enrage", enrageStacks).withStyle(ChatFormatting.DARK_RED));
        }
    }

    // Damage after the soft enrage (+20% per stack)
    private float scaled(float damage) {
        return damage * (1 + 0.2f * enrageStacks);
    }

    // --- death ---

    @Override
    public void die(DamageSource source) {
        if (level() instanceof ServerLevel level) {
            clearAttackState();
            startAction(SlagheartAction.DEATH, 1f);
            restoreBlocks(level);
            rings.clear();
            discardMites(level);
            sound(level, ModSounds.SLAGHEART_DEATH.get(), 3f, 1f);
            announce(level, Component.translatable("message.skillcraft.slagheart_defeated").withStyle(ChatFormatting.GOLD));
            SlagheartRewards.onDefeated(level, this);
        }
        super.die(source);
    }

    @Override
    protected void tickDeath() {
        deathTime++;
        if (level() instanceof ServerLevel level) {
            if (deathTime % 5 == 0) level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1.5, getZ(), 6, 1, 1, 1, 0.02);
            if (deathTime >= DEATH_TICKS) {
                level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY() + 1, getZ(), 1, 0, 0, 0, 0);
                level.sendParticles(ParticleTypes.LAVA, getX(), getY() + 1, getZ(), 40, 1.5, 1, 1.5, 0.2);
                remove(RemovalReason.KILLED);
            }
        }
    }

    @Override
    protected boolean shouldDropLoot(ServerLevel level) {
        return false; // rewards are rolled per player by SlagheartRewards
    }

    @Override
    public void remove(RemovalReason reason) {
        if (level() instanceof ServerLevel level) {
            restoreBlocks(level);
            if (reason != RemovalReason.UNLOADED_TO_CHUNK && reason != RemovalReason.UNLOADED_WITH_PLAYER) discardMites(level);
        }
        bossEvent.removeAllPlayers();
        super.remove(reason);
    }

    // --- client animation ---

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if ((DATA_ACTION.equals(accessor) || DATA_SERIAL.equals(accessor)) && level().isClientSide()) {
            SlagheartAction action = action();
            for (Map.Entry<SlagheartAction, AnimationState> entry : animations.entrySet()) {
                if (entry.getKey() == action) entry.getValue().start(tickCount);
                else entry.getValue().stop();
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) idleAnimation.animateWhen(action() == SlagheartAction.NONE && !walkAnimation.isMoving(), tickCount);
    }

    // --- boss bar ---

    private void updateBossBar() {
        bossEvent.setProgress(getHealth() / getMaxHealth());
        bossEvent.setName(Component.translatable("entity.skillcraft.slagheart")
                .append(Component.translatable("boss.skillcraft.slagheart_phase" + phase())));
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossEvent.removePlayer(player);
    }

    // --- helpers ---

    // Pushes a player away from a point (horizontal strength plus a little lift)
    private static void shove(ServerPlayer player, double fromX, double fromZ, double strength, double lift) {
        Vec3 away = new Vec3(player.getX() - fromX, 0, player.getZ() - fromZ);
        away = away.lengthSqr() < 1.0E-4 ? Vec3.ZERO : away.normalize().scale(strength);
        player.push(away.x, lift, away.z);
        player.hurtMarked = true;
    }

    private void ringParticles(ServerLevel level, Vec3 centre, double radius, net.minecraft.core.particles.SimpleParticleType particle) {
        int points = Math.min(64, Math.max(10, (int) (radius * 6)));
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2 * i / points;
            level.sendParticles(particle, centre.x + Math.cos(angle) * radius, floorY + 1.1, centre.z + Math.sin(angle) * radius, 1, 0, 0.02, 0, 0);
        }
    }

    private void arcParticles(ServerLevel level) {
        for (int i = -4; i <= 4; i++) {
            Vec3 dir = Vec3.directionFromRotation(0, lockedYaw + i * 18);
            level.sendParticles(ParticleTypes.CRIT, getX() + dir.x * SWEEP_RADIUS, floorY + 1.2, getZ() + dir.z * SWEEP_RADIUS, 1, 0, 0, 0, 0);
        }
    }

    private void chimneySmoke(ServerLevel level, int count) {
        Vec3 back = Vec3.directionFromRotation(0, getYRot()).scale(-0.4);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, getX() + back.x, getY() + 3.7, getZ() + back.z, count, 0.5, 0.1, 0.5, 0.02);
    }

    private void playActionSound(ServerLevel level, SlagheartAction action) {
        SoundEvent sound = switch (action) {
            case AWAKEN -> ModSounds.SLAGHEART_AWAKEN.get();
            case HAMMERFALL -> ModSounds.SLAGHEART_HAMMER_WINDUP.get();
            case TONG_SWEEP -> ModSounds.SLAGHEART_SWEEP_WINDUP.get();
            case SLAG_SPIT -> ModSounds.SLAGHEART_INHALE.get();
            case EMBER_RAIN -> ModSounds.SLAGHEART_HAMMER_WINDUP.get();
            case FORGE_QUAKE -> ModSounds.SLAGHEART_QUAKE.get();
            case VENT -> ModSounds.SLAGHEART_VENT.get();
            case ROAR -> ModSounds.SLAGHEART_ROAR.get();
            default -> null;
        };
        if (sound != null) sound(level, sound, 2f, 1f);
    }

    private void sound(ServerLevel level, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, getX(), getY(), getZ(), sound, SoundSource.HOSTILE, volume, pitch);
    }

    private void announce(ServerLevel level, Component message) {
        for (ServerPlayer player : fightPlayers(level)) player.sendSystemMessage(message);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.SLAGHEART_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.SLAGHEART_DEATH.get();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(ModSounds.SLAGHEART_STEP.get(), 0.8f, 0.8f);
    }

    // --- saving (an instance is never resumed after a restart; see customServerAiStep) ---

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("ArenaInstance", instanceId);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        instanceId = input.getIntOr("ArenaInstance", -1);
    }
}
