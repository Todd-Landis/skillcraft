package com.toddlandis.skillcraft.boss;

import com.toddlandis.skillcraft.combat.CombatDamage;
import com.toddlandis.skillcraft.combat.DamageStyle;
import com.toddlandis.skillcraft.registry.ModEntities;
import com.toddlandis.skillcraft.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

// Slagheart's Slag Spit projectile: a slow, arcing glob of molten slag. Hitting a player burns
// them; hitting the floor leaves a short-lived slag puddle (see SlagheartEntity.addHazard).
public class SlagGlob extends ThrowableItemProjectile {
    private float damage = 6f;

    public SlagGlob(EntityType<? extends SlagGlob> type, Level level) {
        super(type, level);
    }

    public SlagGlob(Level level, LivingEntity owner, float damage) {
        super(ModEntities.SLAG_GLOB.get(), owner, level, new ItemStack(ModItems.SLAG_GLOB.get()));
        this.damage = damage;
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.SLAG_GLOB.get();
    }

    @Override
    protected double getDefaultGravity() {
        return 0.035;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            level().addParticle(ParticleTypes.LAVA, getX(), getY(), getZ(), 0, 0, 0);
            level().addParticle(ParticleTypes.SMOKE, getX(), getY(), getZ(), 0, 0.02, 0);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (level() instanceof ServerLevel level && result.getEntity() instanceof LivingEntity target && !(target instanceof SlagheartEntity)) {
            if (CombatDamage.hurtWithStyle(level, target, damageSources().thrown(this, getOwner()), damage, DamageStyle.HEAVY)) {
                target.igniteForSeconds(3);
            }
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.LAVA, getX(), getY(), getZ(), 8, 0.3, 0.1, 0.3, 0.1);
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.LAVA_EXTINGUISH, SoundSource.HOSTILE, 0.8f, 1.2f);
            if (getOwner() instanceof SlagheartEntity boss) boss.addSlagPuddle(BlockPos.containing(getX(), getY() - 0.2, getZ()));
            discard();
        }
    }
}
