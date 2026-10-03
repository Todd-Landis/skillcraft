package com.toddlandis.skillcraft.combat;

import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

// Ammo item for light/heavy arrows; works in bows, crossbows and dispensers like vanilla arrows.
public class SkillArrowItem extends ArrowItem {
    private final SkillArrow.Weight weight;

    public SkillArrowItem(SkillArrow.Weight weight, Properties properties) {
        super(properties);
        this.weight = weight;
    }

    @Override
    public AbstractArrow createArrow(Level level, ItemStack stack, LivingEntity owner, @Nullable ItemStack weapon) {
        return new SkillArrow(weight, level, owner, stack.copyWithCount(1), weapon);
    }

    @Override
    public Projectile asProjectile(Level level, Position position, ItemStack stack, Direction direction) {
        SkillArrow arrow = new SkillArrow(weight, level, position.x(), position.y(), position.z(), stack.copyWithCount(1), null);
        arrow.pickup = AbstractArrow.Pickup.ALLOWED;
        return arrow;
    }
}
