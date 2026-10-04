package com.toddlandis.skillcraft.bonding;

import com.toddlandis.skillcraft.Config;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

// The Cloudray (docs/BEASTBONDING.md section 4): a great sky-manta and Beastbonding's level-62
// flying mount. Wild ones only come from a Cloudray Lure; they glide about for a few minutes, then
// leave unless bonded. A bonded Cloudray hovers where you leave it, and when you ride it, it flies
// where you look: forward to go, look up or down to climb or dive, no input to hover.
public class Cloudray extends PathfinderMob implements BondedMount {
    // Persistent-data tag on a lured wild Cloudray: the game time it arrived
    public static final String LURED_TAG = "skillcraft_lured_at";
    // How quickly flight answers the rider (higher = snappier, lower = more drift)
    private static final double RESPONSE = 0.12;

    public Cloudray(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        moveControl = new FlyingMoveControl(this, 10, true);
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FLYING_SPEED, 0.7)
                .add(Attributes.FOLLOW_RANGE, 24);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanFloat(true);
        return navigation;
    }

    // Wild Cloudrays wander the sky; bonded ones stay put
    @Override
    protected void registerGoals() {
        goalSelector.addGoal(3, new WaterAvoidingRandomFlyingGoal(this, 1.0) {
            @Override
            public boolean canUse() {
                return !BondService.isBonded(Cloudray.this) && super.canUse();
            }
        });
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12f));
    }

    @Override
    public void tick() {
        super.tick();
        setNoGravity(true);
        // A lured wild Cloudray leaves after a while if nobody bonds it
        if (level() instanceof ServerLevel level && !BondService.isBonded(this)) {
            long arrived = getPersistentData().getLongOr(LURED_TAG, -1L);
            if (arrived >= 0 && level.getGameTime() - arrived > Config.CLOUDRAY_WILD_TICKS.get()) {
                level.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.5, getZ(), 20, 1.0, 0.4, 1.0, 0.02);
                discard();
            }
        }
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!isVehicle() && !player.isSecondaryUseActive() && player.getUUID().equals(BondService.ownerOf(this))) {
            if (!level().isClientSide()) player.startRiding(this);
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public LivingEntity getControllingPassenger() {
        return getFirstPassenger() instanceof Player rider ? rider : super.getControllingPassenger();
    }

    @Override
    protected void tickRidden(Player rider, Vec3 input) {
        super.tickRidden(rider, input);
        setRot(rider.getYRot(), rider.getXRot() * 0.5f);
        yRotO = yBodyRot = yHeadRot = getYRot();
    }

    @Override
    protected Vec3 getRiddenInput(Player rider, Vec3 input) {
        return new Vec3(rider.xxa, 0, rider.zza);
    }

    @Override
    protected float getRiddenSpeed(Player rider) {
        return (float) getAttributeValue(Attributes.FLYING_SPEED);
    }

    // Ridden: fly along the rider's view. Otherwise the normal flying AI moves it.
    @Override
    public void travel(Vec3 input) {
        if (!(getControllingPassenger() instanceof Player rider)) {
            super.travel(input);
            return;
        }
        double speed = getAttributeValue(Attributes.FLYING_SPEED);
        Vec3 look = rider.getLookAngle();
        Vec3 right = new Vec3(-look.z, 0, look.x).normalize();
        Vec3 wanted = look.scale(input.z * speed).add(right.scale(input.x * speed * 0.5));
        Vec3 velocity = getDeltaMovement().lerp(wanted, RESPONSE);
        setDeltaMovement(velocity);
        move(MoverType.SELF, velocity);
    }

    // Getting off in mid-air: a gentle float down instead of a fall
    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        if (!level().isClientSide() && passenger instanceof Player rider && !onGround()) {
            rider.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 200, 0, false, false));
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }
}
