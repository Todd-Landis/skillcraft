package com.toddlandis.skillcraft.combat;

import com.toddlandis.skillcraft.registry.ModEntities;
import com.toddlandis.skillcraft.registry.ModItems;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

// Light and heavy arrows. Vanilla arrows count as medium. Weight changes damage, flight speed
// and drop; its damage type (light/heavy) drives monster weaknesses (see docs/COMBAT.md).
public class SkillArrow extends AbstractArrow {
    public enum Weight {
        LIGHT(DamageStyle.LIGHT, 1.3, 1.2f, 0.04),
        HEAVY(DamageStyle.HEAVY, 3.0, 0.8f, 0.065);

        private final DamageStyle style;
        private final double baseDamage;
        private final float velocity;
        private final double gravity;

        Weight(DamageStyle style, double baseDamage, float velocity, double gravity) {
            this.style = style;
            this.baseDamage = baseDamage;
            this.velocity = velocity;
            this.gravity = gravity;
        }

        public DamageStyle style() {
            return style;
        }

        public double baseDamage() {
            return baseDamage;
        }
    }

    public SkillArrow(EntityType<? extends SkillArrow> type, Level level) {
        super(type, level);
        setBaseDamage(weight().baseDamage);
    }

    public SkillArrow(Weight weight, Level level, LivingEntity owner, ItemStack pickup, @Nullable ItemStack weapon) {
        super(typeFor(weight), owner, level, pickup, weapon);
        setBaseDamage(weight.baseDamage);
    }

    public SkillArrow(Weight weight, Level level, double x, double y, double z, ItemStack pickup, @Nullable ItemStack weapon) {
        super(typeFor(weight), x, y, z, level, pickup, weapon);
        setBaseDamage(weight.baseDamage);
    }

    private static EntityType<SkillArrow> typeFor(Weight weight) {
        return weight == Weight.LIGHT ? ModEntities.LIGHT_ARROW.get() : ModEntities.HEAVY_ARROW.get();
    }

    public Weight weight() {
        return getType() == ModEntities.HEAVY_ARROW.get() ? Weight.HEAVY : Weight.LIGHT;
    }

    // Light arrows fly faster and flatter, heavy ones slower with more drop
    @Override
    public void shoot(double x, double y, double z, float velocity, float inaccuracy) {
        super.shoot(x, y, z, velocity * weight().velocity, inaccuracy);
    }

    // Heavy arrows shove harder: one extra level of knockback (like Punch I), reduced by knockback resistance
    @Override
    protected void doKnockback(LivingEntity target, DamageSource source) {
        super.doKnockback(target, source);
        if (weight() != Weight.HEAVY) return;
        double resist = Math.max(0, 1 - target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
        Vec3 push = getDeltaMovement().multiply(1, 0, 1).normalize().scale(0.6 * resist);
        if (push.lengthSqr() > 0) target.push(push.x, 0.1 * resist, push.z);
    }

    @Override
    protected double getDefaultGravity() {
        return weight().gravity;
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(weight() == Weight.HEAVY ? ModItems.HEAVY_ARROW.get() : ModItems.LIGHT_ARROW.get());
    }
}
