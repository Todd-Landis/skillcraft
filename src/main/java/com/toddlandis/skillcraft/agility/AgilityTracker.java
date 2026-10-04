package com.toddlandis.skillcraft.agility;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

import com.toddlandis.skillcraft.Config;
import com.toddlandis.skillcraft.SkillCraft;
import com.toddlandis.skillcraft.skill.Skill;
import com.toddlandis.skillcraft.skill.SkillManager;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

// Agility (docs/AGILITY_MAGIC_PRAYER.md section 4): XP for moving well, and two passive perks.
//   Feats: climbing (height gained on ladders/vines/scaffolding), gap jumps (sprint jumps over a
//   drop) and high dives (a long fall into water). Each pays once per spot (a 4x4x4 cell) per
//   parkourNoveltyTicks, so running back and forth over the same jump stops paying.
//   Perks: falls hurt later (Sure footing) and sprinting is slightly faster (Fleet feet).
// Everything runs on the server from the player's positions; it also remembers which way each
// player has been moving, for Sidestep.
@EventBusSubscriber(modid = SkillCraft.MODID)
public final class AgilityTracker {
    public static final double CLIMB_XP_PER_BLOCK = 1.0, CLIMB_MAX_XP = 30, CLIMB_MIN_BLOCKS = 2;
    public static final double GAP_MIN = 3.0, GAP_XP = 6, GAP_XP_PER_BLOCK = 2, GAP_MAX_XP = 20;
    public static final double DIVE_MIN_FALL = 10, DIVE_XP = 10;
    public static final Identifier SPRINT_ID = Identifier.fromNamespaceAndPath(SkillCraft.MODID, "agility_sprint");

    enum Feat { CLIMB, GAP, DIVE }

    static final class State {
        Vec3 prev;
        Vec3 movement = Vec3.ZERO;
        boolean wasOnGround = true, wasClimbing;
        double climbStart, climbMax;
        Vec3 takeoff, lastGround;
        boolean sprintTakeoff;
        double airPeak;
        final Map<Long, Long> paid = new HashMap<>();
    }

    private static final Map<Player, State> STATES = new WeakHashMap<>();

    private AgilityTracker() {}

    static State state(Player player) {
        return STATES.computeIfAbsent(player, p -> new State());
    }

    // Which way the player has been moving lately, in blocks per tick (zero if unknown)
    public static Vec3 recentMovement(Player player) {
        State state = STATES.get(player);
        return state == null ? Vec3.ZERO : state.movement;
    }

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) tick(player);
    }

    // One tick of tracking (public so GameTests can drive fake players, which are never ticked)
    public static void tick(ServerPlayer player) {
        State state = state(player);
        Vec3 pos = player.position();
        if (state.prev == null) state.prev = pos;
        state.movement = state.movement.scale(0.6).add(pos.subtract(state.prev).scale(0.4));
        state.prev = pos;
        updateSprint(player);

        boolean tracked = !player.isSpectator() && !player.isCreative() && !player.getAbilities().flying && !player.isPassenger() && !player.isFallFlying();
        boolean onGround = player.onGround();
        boolean climbing = player.onClimbable();
        long now = player.level().getGameTime();

        // Climbing: count the height gained, paid when you step off
        if (climbing) {
            if (!state.wasClimbing) state.climbStart = state.climbMax = pos.y;
            state.climbMax = Math.max(state.climbMax, pos.y);
        } else if (state.wasClimbing) {
            double gained = state.climbMax - state.climbStart;
            if (tracked && gained >= CLIMB_MIN_BLOCKS) {
                award(player, state, Feat.CLIMB, BlockPos.containing(pos.x, state.climbStart, pos.z), Math.min(CLIMB_MAX_XP, Math.floor(gained) * CLIMB_XP_PER_BLOCK), now);
            }
        }

        // Jumps and falls: remember the takeoff, judge the landing
        if (state.wasOnGround && !onGround && !climbing && !player.isInWater()) {
            state.takeoff = state.lastGround != null ? state.lastGround : pos;
            state.sprintTakeoff = player.isSprinting();
            state.airPeak = pos.y;
        }
        if (!onGround && state.takeoff != null) state.airPeak = Math.max(state.airPeak, pos.y);
        if (state.takeoff != null && player.isInWater()) {
            if (tracked && state.airPeak - pos.y >= DIVE_MIN_FALL) award(player, state, Feat.DIVE, BlockPos.containing(pos), DIVE_XP, now);
            state.takeoff = null;
        } else if (state.takeoff != null && onGround && !state.wasOnGround) {
            Vec3 from = state.takeoff;
            double horizontal = pos.subtract(from).horizontalDistance();
            double rise = pos.y - from.y;
            if (tracked && state.sprintTakeoff && horizontal >= GAP_MIN && rise > -2.0 && rise < 1.3 && gapBetween(player, from, pos)) {
                double xp = Math.min(GAP_MAX_XP, GAP_XP + GAP_XP_PER_BLOCK * Math.floor(horizontal - GAP_MIN));
                award(player, state, Feat.GAP, BlockPos.containing(from), xp, now);
            }
            state.takeoff = null;
        } else if (climbing) {
            state.takeoff = null;
        }

        if (onGround && !climbing) state.lastGround = pos;
        state.wasOnGround = onGround;
        state.wasClimbing = climbing;
    }

    // True if there's a drop under the middle of the jump: nothing solid 1-3 blocks below the takeoff height
    static boolean gapBetween(Player player, Vec3 from, Vec3 to) {
        Vec3 middle = from.lerp(to, 0.5);
        int top = (int) Math.floor(from.y) - 1;
        for (int y = top; y >= top - 2; y--) {
            BlockPos pos = BlockPos.containing(middle.x, y, middle.z);
            if (!player.level().getBlockState(pos).getCollisionShape(player.level(), pos).isEmpty()) return false;
        }
        return true;
    }

    private static void award(ServerPlayer player, State state, Feat feat, BlockPos where, double xp, long now) {
        int novelty = Config.PARKOUR_NOVELTY_TICKS.get();
        long key = BlockPos.asLong(where.getX() >> 2, where.getY() >> 2, where.getZ() >> 2) * 31 + feat.ordinal();
        Long last = state.paid.get(key);
        if (last != null && now - last < novelty) return;
        state.paid.put(key, now);
        if (state.paid.size() > 256) {
            for (Iterator<Long> it = state.paid.values().iterator(); it.hasNext(); ) {
                if (now - it.next() >= novelty) it.remove();
            }
        }
        SkillManager.awardXp(player, Skill.AGILITY, xp);
    }

    // Fleet feet: a small sprint speed bonus that grows with Agility
    private static void updateSprint(ServerPlayer player) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) return;
        double bonus = Config.SPRINT_SPEED_PER_LEVEL.get() * (SkillManager.level(player, Skill.AGILITY) - 1);
        boolean want = player.isSprinting() && bonus > 0;
        AttributeModifier current = speed.getModifier(SPRINT_ID);
        if (!want) {
            if (current != null) speed.removeModifier(SPRINT_ID);
        } else if (current == null || current.amount() != bonus) {
            speed.addOrUpdateTransientModifier(new AttributeModifier(SPRINT_ID, bonus, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
    }

    // Sure footing: falls start hurting later
    @SubscribeEvent
    static void onFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        double safe = Config.SAFE_FALL_PER_LEVEL.get() * SkillManager.level(player, Skill.AGILITY);
        if (safe > 0) event.setDistance(Math.max(0, event.getDistance() - safe));
    }
}
