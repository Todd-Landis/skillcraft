package com.toddlandis.skillcraft.bonding;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

// The Cliffstrider (docs/BEASTBONDING.md): a bonded spider, summoned as this tame version. It looks
// like a spider (same model and texture) but never attacks, waits where you leave it, and its
// bonder can ride it, up walls too: while ridden it climbs whenever it walks into one.
public class Cliffstrider extends Spider implements BondedMount {
    public Cliffstrider(EntityType<? extends Spider> type, Level level) {
        super(type, level);
    }

    // No hunting goals at all: just float, look around, and wait for its rider
    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
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
        return new Vec3(rider.xxa * 0.5, 0, rider.zza < 0 ? rider.zza * 0.4 : rider.zza);
    }

    @Override
    protected float getRiddenSpeed(Player rider) {
        return (float) getAttributeValue(Attributes.MOVEMENT_SPEED);
    }

    // While ridden the rider's client moves it, so climb on that side's own wall contact
    @Override
    public boolean onClimbable() {
        return isVehicle() ? horizontalCollision : super.onClimbable();
    }
}
