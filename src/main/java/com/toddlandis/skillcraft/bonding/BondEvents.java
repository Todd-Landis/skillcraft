package com.toddlandis.skillcraft.bonding;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.toddlandis.skillcraft.Config;
import com.toddlandis.skillcraft.SkillCraft;
import com.toddlandis.skillcraft.registry.ModItems;
import com.toddlandis.skillcraft.skill.Skill;
import com.toddlandis.skillcraft.skill.SkillManager;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

// The world side of Beastbonding (docs/BEASTBONDING.md section 7): bonding with the codex, deaths,
// no loot from bonded creatures, putting summons away on logout and dimension change, removing
// stray copies, the helper AI for changed companions, and riding and companion XP.
@EventBusSubscriber(modid = SkillCraft.MODID)
public final class BondEvents {
    private BondEvents() {}

    // Distance ridden on a bonded mount since the last XP payout, and the mount's last position
    private static final Map<UUID, Double> RIDDEN = new HashMap<>();
    private static final Map<UUID, Vec3> LAST_MOUNT_POS = new HashMap<>();

    // Using the Bond Codex on a creature bonds it. Runs before the creature's own interaction, so
    // a tamed wolf doesn't just sit down instead.
    @SubscribeEvent
    static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (!event.getItemStack().is(ModItems.BOND_CODEX.get()) || !(event.getTarget() instanceof LivingEntity target)) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (event.getEntity() instanceof ServerPlayer player) BondService.bond(player, target);
    }

    @SubscribeEvent
    static void onDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel level)) return;
        UUID owner = BondService.ownerOf(entity);
        if (owner == null) return;
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(owner);
        if (player != null) BondService.fallen(player, entity);
    }

    // A bonded creature's saddle, armour and chest stay with its codex entry
    @SubscribeEvent
    static void onDrops(LivingDropsEvent event) {
        if (BondService.isBonded(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent
    static void onXpDrop(LivingExperienceDropEvent event) {
        if (BondService.isBonded(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent
    static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) BondService.storeAll(player);
        RIDDEN.remove(event.getEntity().getUUID());
        LAST_MOUNT_POS.remove(event.getEntity().getUUID());
    }

    // After a crash a bond can still say Out with nothing in the world: put it back in the codex
    @SubscribeEvent
    static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        BondCodex codex = BondService.codex(player);
        boolean changed = false;
        for (BondCodex.Bond bond : codex.bonds()) {
            if (bond.state == BondCodex.State.OUT && BondService.findLive(player.level().getServer(), bond.entity) == null) {
                bond.state = BondCodex.State.STORED;
                bond.entity = null;
                changed = true;
            }
        }
        if (changed) BondService.save(player, codex);
    }

    // Summons left behind in the old dimension go back into the codex (a mount ridden through stays)
    @SubscribeEvent
    static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        BondCodex codex = BondService.codex(player);
        for (BondCodex.Bond bond : codex.bonds()) {
            if (bond.state != BondCodex.State.OUT) continue;
            Entity live = BondService.findLive(player.level().getServer(), bond.entity);
            if (live == null || live.level() != player.level()) BondService.store(player, bond);
        }
    }

    @SubscribeEvent
    static void onJoin(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        if (!(event.getLevel() instanceof ServerLevel level) || !BondService.isBonded(entity)) return;
        // A bonded creature loaded from disk must be the one its owner's codex says is Out;
        // anything else is a stray copy (left behind by a crash) and is removed
        if (event.loadedFromDisk()) {
            ServerPlayer owner = level.getServer().getPlayerList().getPlayer(BondService.ownerOf(entity));
            if (owner == null || BondService.codex(owner).findByEntity(entity.getUUID()) == null) {
                event.setCanceled(true);
                return;
            }
        }
        // Changed companions (not vanilla pets) follow their bonder; a fox also fetches
        if (entity instanceof Mob mob && !(entity instanceof TamableAnimal) && !(entity instanceof net.minecraft.world.entity.animal.allay.Allay)
                && !(entity instanceof BondedMount)) {
            mob.goalSelector.addGoal(1, new FollowBonderGoal(mob, 1.25));
            if (entity instanceof Fox) {
                mob.goalSelector.addGoal(2, new FetchItemsGoal(mob));
                // a bonded fox stops hunting chickens and rabbits
                mob.targetSelector.removeAllGoals(goal -> goal instanceof net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<?>);
            }
        }
    }

    // Companions at work: XP for the damage they deal to monsters
    @SubscribeEvent
    static void onDamage(LivingDamageEvent.Post event) {
        Entity attacker = event.getSource().getEntity();
        if (attacker == null || !(event.getEntity() instanceof Enemy) || !(attacker.level() instanceof ServerLevel level)) return;
        UUID owner = BondService.ownerOf(attacker);
        if (owner == null) return;
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(owner);
        if (player != null) SkillManager.awardXp(player, Skill.BEASTBONDING, event.getNewDamage() * Config.BOND_COMPANION_XP_PER_DAMAGE.get());
    }

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.tickCount % 20 == 0) BondService.tick(player);
        rideXp(player);
    }

    // Riding a bonded mount: its ride XP per 100 blocks travelled (horizontally)
    static void rideXp(ServerPlayer player) {
        Entity vehicle = player.getVehicle();
        if (vehicle == null || !player.getUUID().equals(BondService.ownerOf(vehicle))) {
            LAST_MOUNT_POS.remove(player.getUUID());
            return;
        }
        Vec3 now = vehicle.position();
        Vec3 last = LAST_MOUNT_POS.put(player.getUUID(), now);
        if (last == null) return;
        double moved = Math.sqrt(Math.pow(now.x - last.x, 2) + Math.pow(now.z - last.z, 2));
        if (moved > 4) return; // a teleport, not riding
        double total = RIDDEN.merge(player.getUUID(), moved, Double::sum);
        if (total < 100) return;
        RIDDEN.put(player.getUUID(), total - 100);
        Bondable data = BondService.bondable(vehicle.getType());
        BondCodex.Bond bond = BondService.codex(player).findByEntity(vehicle.getUUID());
        if (data == null && bond != null) data = BondService.bondable(bond.species);
        if (data != null && data.rideXp() > 0) SkillManager.awardXp(player, Skill.BEASTBONDING, data.rideXp());
    }
}
