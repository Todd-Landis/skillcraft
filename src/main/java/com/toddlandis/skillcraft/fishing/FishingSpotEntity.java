package com.toddlandis.skillcraft.fishing;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.toddlandis.skillcraft.node.NodeType;
import com.toddlandis.skillcraft.registry.ModEntities;
import com.toddlandis.skillcraft.registry.ModItems;
import com.toddlandis.skillcraft.registry.ModSounds;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

// A fishing spot: a patch of churning, bubbling water that fish gather in. It is an entity rather
// than a block so it sits *on* the water with no block outline, like the fishing spots in classic
// MMOs. Right-click it with a fishing rod to start fishing (see FishingSessions).
//
// Depleting it doesn't leave a stump: the shoal swims off (disappears) and resurfaces on another
// tile of the same pond after the node's regrow time. If the pond is drained or built over, the
// shoal pops back out as an item.
public class FishingSpotEntity extends Entity {
    private static final EntityDataAccessor<Integer> DATA_TYPE = SynchedEntityData.defineId(FishingSpotEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_DEPLETED = SynchedEntityData.defineId(FishingSpotEntity.class, EntityDataSerializers.BOOLEAN);

    // The entity sits this far above the bottom of its water block; the water surface is at ~0.89
    public static final double SURFACE_OFFSET = 0.8;
    public static final double SURFACE_Y = 0.89;
    private static final int CHECK_INTERVAL = 40;

    private long resurfaceAt;

    public FishingSpotEntity(EntityType<? extends FishingSpotEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public static FishingSpotEntity create(ServerLevel level, NodeType type, BlockPos water) {
        FishingSpotEntity spot = new FishingSpotEntity(ModEntities.FISHING_SPOT.get(), level);
        spot.entityData.set(DATA_TYPE, type.ordinal());
        spot.placeOn(water);
        return spot;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_TYPE, NodeType.COD.ordinal());
        builder.define(DATA_DEPLETED, false);
    }

    public NodeType type() {
        int index = entityData.get(DATA_TYPE);
        NodeType[] types = NodeType.values();
        return index >= 0 && index < types.length && types[index].isSpot() ? types[index] : NodeType.COD;
    }

    public boolean isDepleted() {
        return entityData.get(DATA_DEPLETED);
    }

    // The water block the spot floats on
    public BlockPos waterPos() {
        return BlockPos.containing(getX(), getY(), getZ());
    }

    private void placeOn(BlockPos water) {
        setPos(water.getX() + 0.5, water.getY() + SURFACE_OFFSET, water.getZ() + 0.5);
    }

    // --- Depletion: the shoal swims off and resurfaces elsewhere in the pond later ---

    public void deplete(ServerLevel level) {
        level.playSound(null, getX(), getY(), getZ(), ModSounds.SHOAL_DEPLETE.get(), SoundSource.NEUTRAL, 1.0f, 1.0f);
        level.sendParticles(ParticleTypes.SPLASH, getX(), waterPos().getY() + SURFACE_Y, getZ(), 30, 0.5, 0.1, 0.5, 0.2);
        entityData.set(DATA_DEPLETED, true);
        resurfaceAt = level.getGameTime() + type().respawnTicks();
    }

    private void resurface(ServerLevel level) {
        BlockPos here = waterPos();
        Set<BlockPos> taken = new HashSet<>();
        taken.add(here);
        for (FishingSpotEntity other : level.getEntitiesOfClass(FishingSpotEntity.class, new AABB(here).inflate(FishingWater.SCAN_RADIUS + 1), e -> e != this)) {
            taken.add(other.waterPos());
        }
        List<BlockPos> candidates = FishingWater.resurfaceCandidates(level, here, type().minWaterArea(), taken);
        if (!candidates.isEmpty()) {
            placeOn(candidates.get(random.nextInt(candidates.size())));
        } else if (!FishingWater.isValid(level, here, type().minWaterArea())) {
            popOff(level);
            return;
        }
        entityData.set(DATA_DEPLETED, false);
        level.playSound(null, getX(), getY(), getZ(), ModSounds.SHOAL_RESURFACE.get(), SoundSource.NEUTRAL, 0.8f, 1.0f);
        level.sendParticles(ParticleTypes.BUBBLE_POP, getX(), waterPos().getY() + SURFACE_Y, getZ(), 20, 0.4, 0.05, 0.4, 0.02);
    }

    // The water went away: try the rest of the pond, otherwise drop the shoal item
    private void waterChanged(ServerLevel level) {
        Set<BlockPos> taken = Set.of(waterPos());
        List<BlockPos> candidates = FishingWater.resurfaceCandidates(level, waterPos(), type().minWaterArea(), taken);
        if (candidates.isEmpty()) {
            popOff(level);
        } else {
            placeOn(candidates.get(random.nextInt(candidates.size())));
        }
    }

    private void popOff(ServerLevel level) {
        ItemEntity item = new ItemEntity(level, getX(), getY() + 0.3, getZ(), new ItemStack(ModItems.NODES.get(type()).get()));
        item.setDeltaMovement(0, 0.25, 0);
        level.addFreshEntity(item);
        discard();
    }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel level) {
            if (isDepleted()) {
                if (level.getGameTime() >= resurfaceAt) resurface(level);
            } else if (tickCount % CHECK_INTERVAL == 0 && !FishingWater.isValid(level, waterPos(), type().minWaterArea())) {
                waterChanged(level);
            }
        } else if (!isDepleted()) {
            clientEffects();
        }
    }

    // Bubbles, foam and the odd fish breaking the surface
    private void clientEffects() {
        RandomSource r = random;
        double y = waterPos().getY() + SURFACE_Y + 0.02;
        if (r.nextFloat() < 0.7f) {
            level().addParticle(ParticleTypes.BUBBLE_POP, getX() + (r.nextDouble() - 0.5) * 1.2, y, getZ() + (r.nextDouble() - 0.5) * 1.2, 0, 0.02, 0);
        }
        if (r.nextFloat() < 0.35f) {
            float angle = r.nextFloat() * Mth.TWO_PI;
            double dist = 0.2 + r.nextDouble() * 0.5;
            level().addParticle(ParticleTypes.FISHING, getX() + Mth.sin(angle) * dist, y, getZ() + Mth.cos(angle) * dist,
                    Mth.cos(angle) * 0.03, 0.01, -Mth.sin(angle) * 0.03);
        }
        if (r.nextFloat() < 0.08f) {
            level().addParticle(ParticleTypes.SPLASH, getX() + (r.nextDouble() - 0.5) * 0.8, y, getZ() + (r.nextDouble() - 0.5) * 0.8, 0, 0.1, 0);
        }
        if (r.nextFloat() < 0.012f) {
            // A fish flips at the surface
            for (int i = 0; i < 6; i++) {
                level().addParticle(ParticleTypes.SPLASH, getX() + (r.nextDouble() - 0.5) * 0.4, y + 0.1, getZ() + (r.nextDouble() - 0.5) * 0.4, 0, 0.15, 0);
            }
            level().playLocalSound(getX(), y, getZ(), SoundEvents.FISHING_BOBBER_SPLASH, SoundSource.NEUTRAL, 0.12f, 1.2f + r.nextFloat() * 0.4f, false);
        }
    }

    // --- Interaction ---

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (player instanceof ServerPlayer serverPlayer && level() instanceof ServerLevel level) {
            if (player.getMainHandItem().is(ModItems.RELOCATOR.get())) {
                ItemStack shoal = new ItemStack(ModItems.NODES.get(type()).get());
                if (!player.getInventory().add(shoal)) player.drop(shoal, false);
                level.playSound(null, getX(), getY(), getZ(), SoundEvents.ITEM_PICKUP, SoundSource.NEUTRAL, 0.8f, 0.8f);
                serverPlayer.sendOverlayMessage(Component.translatable("message.skillcraft.node_relocated").withStyle(ChatFormatting.GRAY));
                discard();
            } else {
                FishingSessions.start(serverPlayer, this);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean isPickable() {
        return !isDepleted();
    }

    @Override
    public boolean canBeHitByProjectile() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(ModItems.NODES.get(type()).get());
    }

    // --- Saving ---

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        String name = input.getStringOr("node", NodeType.COD.getSerializedName());
        NodeType type = NodeType.COD;
        for (NodeType candidate : NodeType.values()) {
            if (candidate.isSpot() && candidate.getSerializedName().equals(name)) type = candidate;
        }
        entityData.set(DATA_TYPE, type.ordinal());
        entityData.set(DATA_DEPLETED, input.getBooleanOr("depleted", false));
        resurfaceAt = input.getLongOr("resurface_at", 0L);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putString("node", type().getSerializedName());
        output.putBoolean("depleted", isDepleted());
        output.putLong("resurface_at", resurfaceAt);
    }
}
