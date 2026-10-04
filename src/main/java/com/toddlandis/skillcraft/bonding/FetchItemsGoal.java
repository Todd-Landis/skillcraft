package com.toddlandis.skillcraft.bonding;

import java.util.EnumSet;
import java.util.List;

import com.toddlandis.skillcraft.Config;
import com.toddlandis.skillcraft.skill.Skill;
import com.toddlandis.skillcraft.skill.SkillManager;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

// A bonded fox fetches: it runs to items lying within RANGE of its bonder and brings them straight
// into the bonder's inventory (whatever doesn't fit stays on the ground). Pays a little Beastbonding XP.
public class FetchItemsGoal extends Goal {
    private static final double RANGE = 10;
    private static final double REACH = 1.6;

    private final Mob mob;
    private ServerPlayer owner;
    private ItemEntity target;
    private int cooldown;

    public FetchItemsGoal(Mob mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (--cooldown > 0) return false;
        cooldown = 10;
        owner = FollowBonderGoal.owner(mob);
        if (owner == null) return false;
        target = nearestItem(owner);
        return target != null;
    }

    private ItemEntity nearestItem(ServerPlayer owner) {
        List<ItemEntity> items = mob.level().getEntitiesOfClass(ItemEntity.class, owner.getBoundingBox().inflate(RANGE),
                item -> item.isAlive() && !item.hasPickUpDelay() && !item.getItem().isEmpty());
        ItemEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (ItemEntity item : items) {
            double d = item.distanceToSqr(mob);
            if (d < bestDist) {
                bestDist = d;
                best = item;
            }
        }
        return best;
    }

    @Override
    public boolean canContinueToUse() {
        return owner != null && target != null && target.isAlive() && owner.isAlive() && target.distanceToSqr(owner) < (RANGE + 4) * (RANGE + 4);
    }

    @Override
    public void stop() {
        target = null;
        mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (mob.distanceToSqr(target) > REACH * REACH) {
            mob.getNavigation().moveTo(target, 1.3);
            return;
        }
        if (fetch(owner, target)) {
            mob.level().playSound(null, mob.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.NEUTRAL, 0.4f, 1.4f);
            SkillManager.awardXp(owner, Skill.BEASTBONDING, Config.BOND_FETCH_XP.get());
        }
        target = null;
    }

    // Moves the item stack into the player's inventory; true if any of it went in
    public static boolean fetch(ServerPlayer owner, ItemEntity item) {
        ItemStack stack = item.getItem().copy();
        int before = stack.getCount();
        owner.getInventory().add(stack);
        if (stack.isEmpty()) {
            item.discard();
        } else {
            item.setItem(stack);
        }
        return stack.getCount() < before;
    }
}
