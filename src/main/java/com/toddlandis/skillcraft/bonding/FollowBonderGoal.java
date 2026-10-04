package com.toddlandis.skillcraft.bonding;

import java.util.EnumSet;
import java.util.UUID;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

// Keeps a bonded companion that isn't a vanilla pet (fox, snow golem, iron golem) at its bonder's
// side: walks over when it's more than START blocks away, and catches up instantly past TELEPORT.
public class FollowBonderGoal extends Goal {
    private static final double START = 6;
    private static final double STOP = 3;
    private static final double TELEPORT = 24;

    private final Mob mob;
    private final double speed;
    private ServerPlayer owner;
    private int repath;

    public FollowBonderGoal(Mob mob, double speed) {
        this.mob = mob;
        this.speed = speed;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    static ServerPlayer owner(Mob mob) {
        UUID id = BondService.ownerOf(mob);
        if (id == null || !(mob.level() instanceof ServerLevel level)) return null;
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(id);
        return player != null && player.level() == mob.level() ? player : null;
    }

    @Override
    public boolean canUse() {
        if (mob.isVehicle() || mob.isPassenger()) return false;
        owner = owner(mob);
        return owner != null && !owner.isSpectator() && mob.distanceToSqr(owner) > START * START;
    }

    @Override
    public boolean canContinueToUse() {
        return owner != null && owner.isAlive() && owner.level() == mob.level() && mob.distanceToSqr(owner) > STOP * STOP && !mob.isVehicle();
    }

    @Override
    public void start() {
        repath = 0;
    }

    @Override
    public void stop() {
        owner = null;
        mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        mob.getLookControl().setLookAt(owner, 10f, mob.getMaxHeadXRot());
        if (--repath > 0) return;
        repath = 10;
        if (mob.distanceToSqr(owner) > TELEPORT * TELEPORT) {
            mob.snapTo(owner.getX(), owner.getY(), owner.getZ(), mob.getYRot(), mob.getXRot());
            mob.getNavigation().stop();
        } else {
            mob.getNavigation().moveTo(owner, speed);
        }
    }
}
