package com.toddlandis.skillcraft.ability;

import java.util.Comparator;
import java.util.List;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

// Finding targets and safe destinations. Everything raycasts against blocks, so abilities never
// hit through walls and movement never passes through them (or out of a boss arena, whose walls
// and dome are solid or barrier blocks).
public final class AbilityTargeting {
    private static final double SKIN = 0.35; // stop this far short of a wall

    private AbilityTargeting() {}

    // The enemy under the crosshair within melee reach, if there's a clear line to it
    public static LivingEntity meleeTarget(ServerPlayer player, double extraReach) {
        double reach = player.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE) + extraReach;
        return lookTarget(player, reach);
    }

    public static LivingEntity lookTarget(ServerPlayer player, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getViewVector(1f).scale(range));
        BlockHitResult wall = player.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (wall.getType() != HitResult.Type.MISS) end = wall.getLocation();
        AABB box = player.getBoundingBox().expandTowards(end.subtract(eye)).inflate(1.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, eye, end, box,
                e -> e instanceof LivingEntity living && CrowdControl.canAffect(player, living), eye.distanceToSqr(end));
        return hit != null && hit.getEntity() instanceof LivingEntity living ? living : null;
    }

    // Enemies within a radius of a point (with a clear line from the caster's eyes)
    public static List<LivingEntity> around(ServerPlayer player, Vec3 center, double radius, double height) {
        AABB box = new AABB(center.x - radius, center.y - 1, center.z - radius, center.x + radius, center.y + height, center.z + radius);
        return player.level().getEntitiesOfClass(LivingEntity.class, box, e -> CrowdControl.canAffect(player, e)
                        && e.position().subtract(center).horizontalDistance() <= radius + e.getBbWidth() / 2 && canSee(player, e))
                .stream().sorted(Comparator.comparingDouble(e -> e.distanceToSqr(player))).toList();
    }

    // Enemies in front of the caster within an arc (degrees) and radius
    public static List<LivingEntity> inArc(ServerPlayer player, double radius, double arcDegrees) {
        Vec3 facing = player.getLookAngle().multiply(1, 0, 1).normalize();
        double minDot = Math.cos(Math.toRadians(arcDegrees / 2));
        return around(player, player.position(), radius, 3).stream().filter(e -> {
            Vec3 to = e.position().subtract(player.position()).multiply(1, 0, 1);
            return to.lengthSqr() < 0.25 || to.normalize().dot(facing) >= minDot;
        }).toList();
    }

    // Enemies along a straight line from the eyes (stopping at the first wall), within `width`
    public static List<LivingEntity> inLine(ServerPlayer player, double length, double width) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = clipEnd(player, eye, eye.add(player.getViewVector(1f).scale(length)));
        AABB box = new AABB(eye, end).inflate(width);
        return player.level().getEntitiesOfClass(LivingEntity.class, box, e -> CrowdControl.canAffect(player, e)
                        && e.getBoundingBox().inflate(width).clip(eye, end).isPresent())
                .stream().sorted(Comparator.comparingDouble(e -> e.distanceToSqr(player))).toList();
    }

    public static boolean canSee(ServerPlayer player, LivingEntity target) {
        Vec3 eye = player.getEyePosition();
        Vec3 to = target.getBoundingBox().getCenter();
        return player.level().clip(new ClipContext(eye, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getType() == HitResult.Type.MISS;
    }

    public static Vec3 clipEnd(ServerPlayer player, Vec3 from, Vec3 to) {
        BlockHitResult hit = player.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.MISS ? to : hit.getLocation();
    }

    // How far the player can move horizontally along `direction` (up to `distance`) before a wall,
    // checked at knee and head height and at both sides of the hitbox
    public static double clearDistance(ServerPlayer player, Vec3 direction, double distance) {
        Vec3 dir = direction.multiply(1, 0, 1).normalize();
        Vec3 side = new Vec3(-dir.z, 0, dir.x).scale(player.getBbWidth() / 2 * 0.9);
        double best = distance;
        Vec3 base = player.position();
        for (double h : new double[]{0.4, player.getBbHeight() - 0.2}) {
            for (Vec3 offset : new Vec3[]{Vec3.ZERO, side, side.reverse()}) {
                Vec3 from = base.add(offset).add(0, h, 0);
                Vec3 to = from.add(dir.scale(distance + player.getBbWidth() / 2));
                BlockHitResult hit = player.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
                if (hit.getType() != HitResult.Type.MISS) {
                    best = Math.min(best, hit.getLocation().distanceTo(from) - player.getBbWidth() / 2 - SKIN);
                }
            }
        }
        return Math.max(0, best);
    }
}
