package com.toddlandis.skillcraft.magic;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.toddlandis.skillcraft.ability.AbilityDamage;
import com.toddlandis.skillcraft.ability.CrowdControl;
import com.toddlandis.skillcraft.ability.behavior.EffectSpec;
import com.toddlandis.skillcraft.combat.CombatDamage;
import com.toddlandis.skillcraft.combat.DamageStyle;
import com.toddlandis.skillcraft.registry.ModEntities;
import com.toddlandis.skillcraft.registry.ModItems;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

// A magic bolt: flies straight (no gravity) and deals elemental damage through the combat pipeline.
// It looks like its element's mote. Two kinds:
//   - a plain bolt (right-click with a focus): a normal projectile hit, so it builds Momentum and
//     pays Magic XP like an arrow pays Ranged XP
//   - an ability bolt (a spell such as Fireball): damage goes through AbilityDamage (no extra
//     Momentum, PvP rules), and it can burst (radius), ignite, apply effects and chain
// Its settings aren't saved: a bolt that gets unloaded mid-flight just disappears.
public class SpellBolt extends ThrowableItemProjectile {
    private static final int LIFETIME = 16; // about 30-45 blocks, depending on the speed

    // What the bolt does when it lands (immutable; built by the caster)
    public record Payload(DamageStyle element, float damage, boolean ability, float radius, int ignite, List<EffectSpec> effects,
                          int chains, float chainRange, float chainFalloff, float knockback) {
        public static Payload plain(DamageStyle element, float damage) {
            return new Payload(element, damage, false, 0, 0, List.of(), 0, 0, 0, 0);
        }
    }

    private Payload payload;

    public SpellBolt(EntityType<? extends SpellBolt> type, Level level) {
        super(type, level);
    }

    public SpellBolt(Level level, LivingEntity owner, Payload payload) {
        super(ModEntities.SPELL_BOLT.get(), owner, level, new ItemStack(Motes.of(payload.element())));
        this.payload = payload;
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.ARCANE_MOTE.get();
    }

    @Override
    protected double getDefaultGravity() {
        return 0;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            level().addParticle(trail(), getX(), getY(), getZ(), 0, 0, 0);
        } else if (tickCount > LIFETIME || payload == null || getOwner() == null) {
            discard();
        }
    }

    // The trail matches the element (the client only knows the mote item it shows)
    private ParticleOptions trail() {
        Item item = getItem().getItem();
        if (item == ModItems.EMBER_MOTE.get()) return ParticleTypes.FLAME;
        if (item == ModItems.FROST_MOTE.get()) return ParticleTypes.SNOWFLAKE;
        if (item == ModItems.STORM_MOTE.get()) return ParticleTypes.ELECTRIC_SPARK;
        return ParticleTypes.WITCH;
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return super.canHitEntity(entity) && entity != getOwner();
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!(level() instanceof ServerLevel level) || payload == null) return;
        if (result.getEntity() instanceof LivingEntity target) {
            Set<LivingEntity> hit = new HashSet<>();
            if (payload.radius() <= 0) {
                damage(level, target, payload.damage());
                hit.add(target);
            }
            if (payload.chains() > 0) chain(level, target, hit);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!(level() instanceof ServerLevel level) || payload == null) {
            if (!level().isClientSide()) discard();
            return;
        }
        if (payload.radius() > 0) burst(level, result.getLocation());
        level.sendParticles(trail(), getX(), getY(), getZ(), payload.radius() > 0 ? 30 : 8, 0.25 + payload.radius() / 3, 0.25, 0.25 + payload.radius() / 3, 0.05);
        level.playSound(null, getX(), getY(), getZ(), impactSound(), SoundSource.PLAYERS, payload.radius() > 0 ? 1.0f : 0.6f, 1.2f);
        discard();
    }

    private SoundEvent impactSound() {
        return switch (payload.element()) {
            case FIRE -> payload.radius() > 0 ? SoundEvents.GENERIC_EXPLODE.value() : SoundEvents.FIRECHARGE_USE;
            case FROST -> SoundEvents.GLASS_BREAK;
            case STORM -> SoundEvents.TRIDENT_THUNDER.value();
            default -> SoundEvents.AMETHYST_BLOCK_BREAK;
        };
    }

    // Everything within the radius of the impact (with a clear line from the impact point)
    private void burst(ServerLevel level, Vec3 center) {
        float r = payload.radius();
        AABB box = new AABB(center, center).inflate(r, r, r);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != getOwner() && e.isAlive())) {
            if (target.getBoundingBox().distanceToSqr(center) > r * r) continue;
            Vec3 to = target.getBoundingBox().getCenter();
            if (level.clip(new ClipContext(center, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getType() != HitResult.Type.MISS
                    && !target.getBoundingBox().inflate(0.3).contains(center)) continue;
            damage(level, target, payload.damage());
        }
    }

    // Storm chains: jump to the nearest unhit enemies, losing chainFalloff each time
    private void chain(ServerLevel level, LivingEntity first, Set<LivingEntity> hit) {
        LivingEntity from = first;
        float amount = payload.damage();
        for (int i = 0; i < payload.chains(); i++) {
            amount *= 1 - payload.chainFalloff();
            LivingEntity source = from;
            List<LivingEntity> near = new ArrayList<>(level.getEntitiesOfClass(LivingEntity.class, source.getBoundingBox().inflate(payload.chainRange()),
                    e -> e != getOwner() && e.isAlive() && !hit.contains(e) && e.distanceTo(source) <= payload.chainRange() && allowed(e)));
            if (near.isEmpty()) return;
            near.sort((a, b) -> Double.compare(a.distanceToSqr(source), b.distanceToSqr(source)));
            LivingEntity next = near.getFirst();
            arc(level, source, next);
            damage(level, next, amount);
            hit.add(next);
            from = next;
        }
    }

    private static void arc(ServerLevel level, LivingEntity a, LivingEntity b) {
        Vec3 from = a.getBoundingBox().getCenter(), to = b.getBoundingBox().getCenter();
        for (int s = 0; s <= 8; s++) {
            Vec3 p = from.lerp(to, s / 8.0);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 2, 0.05, 0.05, 0.05, 0.01);
        }
    }

    private boolean allowed(LivingEntity target) {
        return !(getOwner() instanceof Player caster) || CrowdControl.canAffect(caster, target);
    }

    private void damage(ServerLevel level, LivingEntity target, float amount) {
        Entity owner = getOwner();
        boolean landed;
        if (payload.ability() && owner instanceof ServerPlayer player) {
            landed = AbilityDamage.hurt(player, target, amount, payload.element());
        } else {
            if (owner instanceof Player caster && target instanceof Player victim && !caster.canHarmPlayer(victim)) return;
            landed = CombatDamage.hurtWithStyle(level, target, damageSources().thrown(this, owner), amount, payload.element());
        }
        if (!landed) return;
        if (payload.ignite() > 0 && !target.fireImmune()) target.igniteForTicks(payload.ignite());
        for (EffectSpec effect : payload.effects()) effect.apply(target, owner != null ? owner : this);
        if (payload.knockback() > 0) CrowdControl.knockback(target, this, payload.knockback());
    }
}
