package com.toddlandis.skillcraft.ability.behavior;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.toddlandis.skillcraft.ability.AbilityBehavior;
import com.toddlandis.skillcraft.ability.AbilityContext;
import com.toddlandis.skillcraft.ability.AbilityRuntime;
import com.toddlandis.skillcraft.ability.AbilityTargeting;
import com.toddlandis.skillcraft.agility.AgilityTracker;

import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

// Instant moves (Sidestep, Grapple Line, Shadowstep, Blink). Every mode is ray-traced against blocks
// and checks the landing spot is free, so nothing passes through walls or out of an arena.
//   mode: look (forward, level), look_3d (where you look, up or down too), move (the way you're
//         moving, backwards if standing still), block (to the block you're looking at), behind_target
//         (behind the enemy you're looking at, facing it)
//   distance: the most it moves (or how far it looks for a block or an enemy)
//   next_hit_bonus / next_hit_window: your next melee hit within the window deals that much more
public record Dash(Mode mode, float distance, float nextHitBonus, int nextHitWindow) implements AbilityBehavior {
    private static final double SKIN = 0.35;

    public enum Mode implements StringRepresentable {
        LOOK, LOOK_3D, MOVE, BLOCK, BEHIND_TARGET;

        public static final Codec<Mode> CODEC = StringRepresentable.fromEnum(Mode::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final MapCodec<Dash> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Mode.CODEC.fieldOf("mode").forGetter(Dash::mode),
            Codec.floatRange(0.5f, 32).fieldOf("distance").forGetter(Dash::distance),
            Codec.floatRange(0, 2).optionalFieldOf("next_hit_bonus", 0f).forGetter(Dash::nextHitBonus),
            Codec.intRange(0, 200).optionalFieldOf("next_hit_window", 60).forGetter(Dash::nextHitWindow)
    ).apply(i, Dash::new));

    @Override
    public MapCodec<Dash> codec() {
        return CODEC;
    }

    @Override
    public boolean execute(AbilityContext context) {
        ServerPlayer player = context.player();
        Vec3 from = player.position();
        float yaw = player.getYRot();
        Vec3 to;
        switch (mode) {
            case LOOK -> to = along(player, player.getLookAngle().multiply(1, 0, 1));
            case LOOK_3D -> to = along(player, player.getLookAngle());
            case MOVE -> {
                Vec3 move = AgilityTracker.recentMovement(player).multiply(1, 0, 1);
                to = along(player, move.lengthSqr() > 0.0025 ? move : player.getLookAngle().multiply(-1, 0, -1));
            }
            case BLOCK -> to = toBlock(context, player);
            case BEHIND_TARGET -> {
                LivingEntity target = AbilityTargeting.lookTarget(player, distance);
                to = target == null ? null : behind(player, target);
                if (to != null) {
                    Vec3 look = target.position().subtract(to);
                    yaw = (float) Math.toDegrees(Math.atan2(look.z, look.x)) - 90f;
                }
            }
            default -> to = null;
        }
        if (to == null || to.distanceToSqr(from) < 0.25) {
            if (context.runtime().failKey == null) {
                context.runtime().failKey = mode == Mode.BLOCK ? "message.skillcraft.ability_no_grip" : "message.skillcraft.ability_no_room";
            }
            return false;
        }
        trail(context.level(), from, to);
        player.teleportTo(context.level(), to.x, to.y, to.z, java.util.Set.of(), yaw, player.getXRot(), true);
        player.setPos(to.x, to.y, to.z); // fake players ignore teleport packets
        player.setYRot(yaw);
        player.setYHeadRot(yaw);
        player.resetFallDistance();
        Fx.sound(context.level(), player, mode == Mode.BLOCK ? SoundEvents.CROSSBOW_HIT : SoundEvents.ENDERMAN_TELEPORT, 0.6f, 1.6f);
        if (nextHitBonus > 0) {
            AbilityRuntime runtime = context.runtime();
            runtime.counterBonus = nextHitBonus;
            runtime.counterMomentum = 0;
            runtime.counterUntil = context.now() + nextHitWindow;
        }
        return true;
    }

    // Moves up to `distance` along a direction, stopping short of anything solid
    private Vec3 along(ServerPlayer player, Vec3 direction) {
        if (direction.lengthSqr() < 1.0E-6) return null;
        Vec3 dir = direction.normalize();
        double travel = clear(player, dir, distance);
        return free(player, player.position(), dir, travel);
    }

    // How far the player's box can move along `dir` (rays from the feet, middle and head, both sides)
    private static double clear(ServerPlayer player, Vec3 dir, double distance) {
        double half = player.getBbWidth() / 2;
        Vec3 sideAxis = Math.abs(dir.y) > 0.99 ? new Vec3(1, 0, 0) : new Vec3(-dir.z, 0, dir.x).normalize();
        Vec3 side = sideAxis.scale(half * 0.9);
        double best = distance;
        for (double h : new double[]{0.1, player.getBbHeight() / 2, player.getBbHeight() - 0.1}) {
            for (Vec3 offset : new Vec3[]{Vec3.ZERO, side, side.reverse()}) {
                Vec3 start = player.position().add(offset).add(0, h, 0);
                Vec3 end = start.add(dir.scale(distance + half));
                BlockHitResult hit = player.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
                if (hit.getType() != HitResult.Type.MISS) best = Math.min(best, hit.getLocation().distanceTo(start) - half - SKIN);
            }
        }
        return Math.max(0, best);
    }

    // Steps back along the path until the player's box fits (null if nowhere fits)
    private static Vec3 free(ServerPlayer player, Vec3 origin, Vec3 dir, double travel) {
        for (double t = travel; t > 0.4; t -= 0.25) {
            Vec3 spot = origin.add(dir.scale(t));
            if (fits(player, spot)) return spot;
        }
        return null;
    }

    private static boolean fits(ServerPlayer player, Vec3 spot) {
        AABB box = player.getBoundingBox().move(spot.subtract(player.position())).deflate(0.01);
        return player.level().noCollision(player, box);
    }

    // Grapple: the block under the crosshair; land on top of it, or hang just in front of its face
    private Vec3 toBlock(AbilityContext context, ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        BlockHitResult hit = context.level().clip(new ClipContext(eye, eye.add(player.getViewVector(1f).scale(distance)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (hit.getType() == HitResult.Type.MISS) return null;
        Vec3 point = hit.getLocation();
        if (hit.getDirection() == Direction.UP) {
            Vec3 top = point.add(0, 0.05, 0);
            if (fits(player, top)) return top;
        }
        if (hit.getDirection() == Direction.DOWN) return null; // can't hang from a ceiling
        Vec3 face = new Vec3(hit.getDirection().getStepX(), hit.getDirection().getStepY(), hit.getDirection().getStepZ());
        Vec3 front = point.add(face.scale(player.getBbWidth() / 2 + 0.1)).add(0, -player.getBbHeight() * 0.6, 0);
        // Prefer standing on the ledge if the block's top is within reach
        Vec3 ledge = new Vec3(point.x, Math.floor(point.y) + 1.0, point.z).subtract(face.scale(0.3));
        if (ledge.y - point.y < 1.0 && fits(player, ledge)) return ledge;
        if (fits(player, front)) return front;
        Vec3 dir = front.subtract(player.position());
        double length = dir.length();
        return length < 1.0E-3 ? null : free(player, player.position(), dir.normalize(), length);
    }

    // Shadowstep: just behind the target (relative to its facing), with clear lines from the target
    private Vec3 behind(ServerPlayer player, LivingEntity target) {
        Vec3 facing = target.getLookAngle().multiply(1, 0, 1);
        if (facing.lengthSqr() < 1.0E-4) facing = target.position().subtract(player.position()).multiply(1, 0, 1);
        if (facing.lengthSqr() < 1.0E-4) return null;
        Vec3 back = facing.normalize().reverse();
        double gap = target.getBbWidth() / 2 + player.getBbWidth() / 2 + 0.4;
        Vec3 spot = target.position().add(back.scale(gap));
        Vec3 center = target.getBoundingBox().getCenter();
        if (player.level().clip(new ClipContext(center, spot.add(0, 0.5, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getType() != HitResult.Type.MISS) {
            return null;
        }
        return fits(player, spot) ? spot : null;
    }

    private static void trail(ServerLevel level, Vec3 from, Vec3 to) {
        for (int s = 0; s <= 8; s++) {
            Vec3 p = from.lerp(to, s / 8.0);
            level.sendParticles(ParticleTypes.CLOUD, p.x, p.y + 0.5, p.z, 1, 0.1, 0.2, 0.1, 0.01);
        }
        level.sendParticles(ParticleTypes.PORTAL, to.x, to.y + 1, to.z, 12, 0.3, 0.5, 0.3, 0.2);
    }

    @Override
    public List<Component> details() {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("ability.skillcraft.detail.dash_" + mode.getSerializedName(), String.format("%.1f", distance)));
        if (nextHitBonus > 0) {
            lines.add(Component.translatable("ability.skillcraft.detail.next_hit", Math.round(nextHitBonus * 100), String.format("%.1f", nextHitWindow / 20.0)));
        }
        return lines;
    }
}
