package com.toddlandis.skillcraft.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.mojang.authlib.GameProfile;
import com.mojang.serialization.JsonOps;
import com.toddlandis.skillcraft.SkillCraft;
import com.toddlandis.skillcraft.ability.AbilityCategory;
import com.toddlandis.skillcraft.ability.AbilityClass;
import com.toddlandis.skillcraft.ability.AbilityData;
import com.toddlandis.skillcraft.ability.AbilityDefinition;
import com.toddlandis.skillcraft.ability.AbilityManager;
import com.toddlandis.skillcraft.ability.AbilityRuntime;
import com.toddlandis.skillcraft.ability.AbilityService;
import com.toddlandis.skillcraft.ability.AbilityService.Denied;
import com.toddlandis.skillcraft.ability.CrowdControl;
import com.toddlandis.skillcraft.ability.behavior.HealOverTime;
import com.toddlandis.skillcraft.ability.behavior.ToggleEffect;
import com.toddlandis.skillcraft.boss.SlagheartAction;
import com.toddlandis.skillcraft.boss.SlagheartEntity;
import com.toddlandis.skillcraft.combat.CombatDamage;
import com.toddlandis.skillcraft.combat.DamageStyle;
import com.toddlandis.skillcraft.combat.DamageStyles;
import com.toddlandis.skillcraft.registry.ModAttachments;
import com.toddlandis.skillcraft.registry.ModEffects;
import com.toddlandis.skillcraft.registry.ModEntities;
import com.toddlandis.skillcraft.registry.ModItems;
import com.toddlandis.skillcraft.skill.Skill;
import com.toddlandis.skillcraft.skill.SkillManager;
import com.toddlandis.skillcraft.skill.XpTable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

// Ability GameTests (spec 2, 6): validation, Momentum, damage through the combat pipeline, boss
// crowd control, movement stopping at walls, loadout locks in combat, the Spell/Prayer-ready
// framework, and the DPS harness (60 s of auto-attacks alone vs. auto-attacks plus a rotation).
//
// Fake players aren't ticked by the server, so the tests call AbilityService.tick and tick
// effects themselves, and apply the held weapon's attribute modifiers by hand.
public final class AbilityTests {
    private AbilityTests() {}

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(SkillCraft.MODID, path);
    }

    static ServerPlayer player(GameTestHelper helper, String name, Vec3 relative, float yaw) {
        ServerPlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.nameUUIDFromBytes(name.getBytes()), name));
        player.setGameMode(GameType.SURVIVAL);
        Vec3 pos = helper.absoluteVec(relative);
        player.snapTo(pos.x, pos.y, pos.z, yaw, 0f);
        player.setYHeadRot(yaw); // abilities aim with the head (a real client keeps it in step)
        player.setYBodyRot(yaw);
        player.setHealth(player.getMaxHealth());
        player.removeAllEffects();
        player.setData(ModAttachments.ABILITIES, new AbilityData());
        player.setData(ModAttachments.ABILITY_RUNTIME, new AbilityRuntime());
        for (Skill skill : Skill.values()) SkillManager.setXp(player, skill, 0);
        return player;
    }

    // How many of an item are anywhere in the player's inventory
    static int countOf(ServerPlayer player, net.minecraft.world.item.Item item) {
        int total = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(item)) total += stack.getCount();
        }
        return total;
    }

    static void level(ServerPlayer player, Skill skill, int level) {
        SkillManager.setXp(player, skill, XpTable.xpForLevel(level));
    }

    // Puts a weapon in the main hand and applies its attack modifiers (normally done by the entity tick)
    static void hold(ServerPlayer player, ItemStack stack) {
        player.setItemSlot(EquipmentSlot.MAINHAND, stack);
        stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY).forEach(EquipmentSlot.MAINHAND, (attribute, modifier) -> {
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance != null) instance.addOrUpdateTransientModifier(modifier);
        });
    }

    static void face(ServerPlayer player, LivingEntity target) {
        Vec3 to = target.getBoundingBox().getCenter().subtract(player.getEyePosition());
        float yaw = (float) Math.toDegrees(Math.atan2(to.z, to.x)) - 90f;
        float pitch = (float) -Math.toDegrees(Math.atan2(to.y, to.horizontalDistance()));
        player.snapTo(player.getX(), player.getY(), player.getZ(), yaw, pitch);
        player.setYHeadRot(yaw);
        player.setYBodyRot(yaw);
    }

    // What a fake player's tick would do for effects: count them down and remove expired ones
    static void tickEffects(ServerLevel level, LivingEntity entity) {
        for (MobEffectInstance instance : new ArrayList<>(entity.getActiveEffects())) {
            if (!instance.tickServer(level, entity, () -> {})) entity.removeEffect(instance.getEffect());
        }
    }

    static void ok(GameTestHelper helper, Denied result, String what) {
        helper.assertTrue(result == null, what + ": expected it to work, was refused with " + result);
    }

    static void is(GameTestHelper helper, Denied result, Denied expected, String what) {
        helper.assertTrue(result == expected, what + ": expected " + expected + ", got " + result);
    }

    // Why a melee ability found no target (shown in failure messages)
    static String targetDebug(ServerPlayer player, LivingEntity target) {
        return String.format("player %s eye %s look %s yaw %.1f pitch %.1f | target %s box %s | canAffect %s canSee %s lookTarget(10) %s around(4) %d reach %.2f",
                player.position(), player.getEyePosition(), player.getViewVector(1f), player.getYRot(), player.getXRot(), target.position(), target.getBoundingBox(),
                CrowdControl.canAffect(player, target), com.toddlandis.skillcraft.ability.AbilityTargeting.canSee(player, target),
                com.toddlandis.skillcraft.ability.AbilityTargeting.lookTarget(player, 10), com.toddlandis.skillcraft.ability.AbilityTargeting.around(player, player.position(), 4, 3).size(),
                player.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE));
    }

    static float momentum(ServerPlayer player) {
        return AbilityService.data(player).momentum();
    }

    static void setMomentum(ServerPlayer player, float value) {
        AbilityData data = AbilityService.data(player);
        data.setMomentum(value);
        AbilityService.save(player, data);
    }

    // --- Validation: locked, wrong weapon, Codex, cooldown, Momentum, global cooldown, off-GCD ---

    static void validation(GameTestHelper helper) {
        helper.assertTrue(AbilityManager.all().size() >= 14, "expected at least 14 abilities, loaded " + AbilityManager.all().size());
        ServerPlayer player = player(helper, "skillcraft_ability_validate", new Vec3(8, 1, 8), 0f);
        level(player, Skill.MELEE, 1);

        // Locked by level
        is(helper, AbilityService.check(player, id("hamstring"), AbilityManager.get(id("hamstring"))), Denied.LOCKED, "Hamstring at Melee 1");
        // Wrong weapon (Concussive Blow needs a warhammer or mace)
        level(player, Skill.MELEE, 40);
        hold(player, new ItemStack(Items.STICK));
        is(helper, AbilityService.check(player, id("concussive_blow"), AbilityManager.get(id("concussive_blow"))), Denied.WEAPON, "Concussive Blow with a stick");
        hold(player, new ItemStack(ModItems.WARHAMMERS.get("iron").get()));
        ok(helper, AbilityService.check(player, id("concussive_blow"), AbilityManager.get(id("concussive_blow"))), "Concussive Blow with a warhammer");
        // Rending Cut needs a slash weapon: a warhammer crushes
        is(helper, AbilityService.check(player, id("rending_cut"), AbilityManager.get(id("rending_cut"))), Denied.WEAPON, "Rending Cut with a warhammer");
        // Codex
        hold(player, new ItemStack(ModItems.EMBERFALL_MAUL.get()));
        is(helper, AbilityService.check(player, id("forgequake"), AbilityManager.get(id("forgequake"))), Denied.CODEX, "Forgequake without its Codex");
        AbilityData data = AbilityService.data(player);
        data.learn(id("forgequake"));
        AbilityService.save(player, data);
        is(helper, AbilityService.check(player, id("forgequake"), AbilityManager.get(id("forgequake"))), Denied.MOMENTUM, "Forgequake (Ultimate) with no Momentum");
        setMomentum(player, 100);
        ok(helper, AbilityService.check(player, id("forgequake"), AbilityManager.get(id("forgequake"))), "Forgequake with a full bar");
        // Momentum for a Surge (needs 50; Melee 40 discount makes it 48)
        hold(player, new ItemStack(Items.DIAMOND_SWORD));
        setMomentum(player, 40);
        is(helper, AbilityService.check(player, id("sweeping_arc"), AbilityManager.get(id("sweeping_arc"))), Denied.MOMENTUM, "Sweeping Arc at 40 Momentum");
        setMomentum(player, 48);
        ok(helper, AbilityService.check(player, id("sweeping_arc"), AbilityManager.get(id("sweeping_arc"))), "Sweeping Arc at 48 Momentum (Melee 40 discount)");

        // Use Lunge (on the GCD): then cooldown on itself, and the GCD blocks another on-GCD ability
        ok(helper, AbilityService.use(player, id("lunge")), "first Lunge");
        is(helper, AbilityService.use(player, id("lunge")), Denied.COOLDOWN, "second Lunge");
        is(helper, AbilityService.check(player, id("hamstring"), AbilityManager.get(id("hamstring"))), Denied.GCD, "Hamstring right after Lunge");
        // Off-GCD defensives still work during the GCD
        level(player, Skill.DEFENCE, 10);
        ok(helper, AbilityService.use(player, id("brace")), "Brace during the GCD");
        helper.assertTrue(player.hasEffect(ModEffects.BRACED), "Brace didn't apply Braced");
        ok(helper, AbilityService.use(player, id("riposte")), "Riposte during the GCD");
        is(helper, AbilityService.use(player, id("brace")), Denied.COOLDOWN, "second Brace");
        // Momentum: Lunge generated 8 on top of what we had (48 - nothing spent)
        helper.assertTrue(Math.abs(momentum(player) - 56) < 0.01, "Lunge should generate 8 Momentum, have " + momentum(player));
        // The slot path: an empty slot is refused, a filled one works through the bar
        is(helper, AbilityService.useSlot(player, 2), Denied.EMPTY, "empty slot");
        SkillCraft.LOGGER.info("[abilities] validation: {} abilities loaded; locked/weapon/codex/momentum/cooldown/GCD/off-GCD checks pass", AbilityManager.all().size());
        helper.succeed();
    }

    // --- Momentum: per melee hit, per projectile hit, per hit taken (Config values), none from abilities, decay ---

    static void momentum(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = player(helper, "skillcraft_ability_momentum", new Vec3(4, 1, 8), -90f);
        level(player, Skill.MELEE, 30);
        double melee = com.toddlandis.skillcraft.Config.MOMENTUM_PER_MELEE_HIT.get(), ranged = com.toddlandis.skillcraft.Config.MOMENTUM_PER_PROJECTILE_HIT.get(), taken = com.toddlandis.skillcraft.Config.MOMENTUM_PER_HIT_TAKEN.get();
        Zombie zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new Vec3(6, 1, 8));

        zombie.hurtServer(level, level.damageSources().playerAttack(player), 3f);
        helper.assertTrue(Math.abs(momentum(player) - melee) < 0.01, "a full-strength melee hit should give " + melee + " Momentum, have " + momentum(player));
        zombie.invulnerableTime = 0;
        Arrow arrow = new Arrow(level, player, new ItemStack(Items.ARROW), new ItemStack(Items.BOW));
        zombie.hurtServer(level, level.damageSources().arrow(arrow, player), 2f);
        helper.assertTrue(Math.abs(momentum(player) - (melee + ranged)) < 0.01, "a projectile hit should give " + ranged + " Momentum, have " + momentum(player));
        // A fake player can never be hurt (its client never "loads"), so post the event a real hit fires
        float hpBefore = player.getHealth();
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Post(player,
                new net.neoforged.neoforge.common.damagesource.DamageContainer(level.damageSources().mobAttack(zombie), 1f)));
        boolean took = true;
        helper.assertTrue(Math.abs(momentum(player) - (melee + ranged + taken)) < 0.01, "a hit taken should give " + taken + " Momentum, have " + momentum(player)
                + " (hurt=" + took + ", health " + hpBefore + " -> " + player.getHealth() + ", mode " + player.gameMode() + ", invulnerable " + player.isInvulnerable() + ")");
        // Ability damage builds no Momentum by itself
        zombie.invulnerableTime = 0;
        com.toddlandis.skillcraft.ability.AbilityDamage.strike(player, zombie, 0.5, null);
        helper.assertTrue(Math.abs(momentum(player) - (melee + ranged + taken)) < 0.01, "ability damage must not build Momentum, have " + momentum(player));
        helper.assertTrue(AbilityService.data(player).inCombat(level.getGameTime()), "hitting a mob should put you in combat");

        // Decay: once out of combat for the delay, it drains 5 per second
        AbilityData data = AbilityService.data(player);
        data.setCombatUntil(level.getGameTime() - 1000);
        AbilityService.save(player, data);
        float start = momentum(player);
        helper.onEachTick(() -> AbilityService.tick(player));
        helper.runAfterDelay(45, () -> {
            float now = momentum(player);
            helper.assertTrue(now <= start - 9.99 && now >= start - 15.01, "Momentum should drain 10-15 in ~2 s out of combat: " + start + " -> " + now);
            SkillCraft.LOGGER.info("[abilities] momentum: melee +{}, projectile +{}, hit taken +{}, ability hits +0; decay {} -> {} in 45 ticks", melee, ranged, taken, start, now);
            helper.succeed();
        });
    }

    // --- Ability damage goes through CombatDamage: Concussive Blow on a skeleton shows the crush matchup ---

    static void pipeline(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = player(helper, "skillcraft_ability_pipeline", new Vec3(4, 1, 8), -90f);
        level(player, Skill.MELEE, 10);
        hold(player, new ItemStack(ModItems.WARHAMMERS.get("iron").get()));
        Skeleton skeleton = helper.spawnWithNoFreeWill(EntityTypes.SKELETON, new Vec3(6, 1, 8));
        skeleton.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        face(player, skeleton);

        double weapon = com.toddlandis.skillcraft.ability.AbilityDamage.weaponHit(player);
        double matchup = DamageStyles.modifier(skeleton, DamageStyle.CRUSH);
        double levelBonus = 1 + CombatDamage.LEVEL_DAMAGE_BONUS * (10 - 1);
        double style = player.getData(ModAttachments.ATTACK_STYLE).damageDealtMultiplier();
        double expected = weapon * 1.3 * matchup * levelBonus * style;
        float before = skeleton.getHealth();
        double xpBefore = SkillManager.get(player).getXp(Skill.MELEE);
        Denied blow = AbilityService.use(player, id("concussive_blow"));
        helper.assertTrue(blow == null, "Concussive Blow refused with " + blow + "; " + targetDebug(player, skeleton));
        float dealt = before - skeleton.getHealth();
        double xp = SkillManager.get(player).getXp(Skill.MELEE) - xpBefore;
        helper.assertTrue(Math.abs(dealt - expected) < 0.05, "Concussive Blow should deal " + expected + " (weapon " + weapon + " x 1.3 x crush " + matchup
                + " x level " + levelBonus + " x style " + style + "), dealt " + dealt);
        helper.assertTrue(matchup > 1.0, "skeletons should be weak to crush");
        helper.assertTrue(skeleton.hasEffect(ModEffects.STUNNED), "Concussive Blow should stun a normal mob");
        // Combat XP is the normal XP for the damage dealt; nothing extra for using an ability
        helper.assertTrue(Math.abs(xp - dealt * 4.0 * player.getData(ModAttachments.ATTACK_STYLE).offenceShare()) < 0.05, "ability XP should be 4 per damage, got " + xp + " for " + dealt);
        SkillCraft.LOGGER.info("[abilities] pipeline: Concussive Blow on a skeleton dealt {} = weapon {} x 1.30 x crush {} x level {} x style {}; {} Melee XP",
                String.format("%.2f", dealt), String.format("%.2f", weapon), matchup, String.format("%.4f", levelBonus), style, String.format("%.1f", xp));
        helper.succeed();
    }

    // --- Off-hand arts: Twinfang hits with the off-hand blade (its own hit, type and level gate), Torchbrand ignites ---

    static void offhand(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = player(helper, "skillcraft_ability_offhand", new Vec3(4, 1, 8), -90f);
        level(player, Skill.MELEE, 30);
        hold(player, new ItemStack(Items.IRON_SWORD));
        Skeleton skeleton = helper.spawnWithNoFreeWill(EntityTypes.SKELETON, new Vec3(6, 1, 8));
        skeleton.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        face(player, skeleton);
        Identifier twinfang = id("twinfang");

        is(helper, AbilityService.check(player, twinfang, AbilityManager.get(twinfang)), Denied.WEAPON, "Twinfang with an empty off hand");
        player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
        is(helper, AbilityService.check(player, twinfang, AbilityManager.get(twinfang)), Denied.WEAPON, "Twinfang with a shield in the off hand");
        // The off-hand weapon's own level gate: a netherite sword needs Melee 40
        player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.NETHERITE_SWORD));
        float start = skeleton.getHealth();
        is(helper, AbilityService.use(player, twinfang), Denied.NO_TARGET, "Twinfang with an off-hand weapon above your level");
        helper.assertTrue(skeleton.getHealth() == start, "a refused Twinfang must not deal damage");

        ItemStack diamond = new ItemStack(Items.DIAMOND_SWORD);
        player.setItemSlot(EquipmentSlot.OFFHAND, diamond);
        double mainHit = com.toddlandis.skillcraft.ability.AbilityDamage.weaponHit(player);
        double offHit = com.toddlandis.skillcraft.ability.AbilityDamage.offhandHit(player);
        double expectedHit = mainHit - com.toddlandis.skillcraft.ability.AbilityDamage.attackBonus(player.getMainHandItem())
                + com.toddlandis.skillcraft.ability.AbilityDamage.attackBonus(diamond);
        helper.assertTrue(Math.abs(offHit - expectedHit) < 1.0E-4 && offHit > mainHit,
                "the off-hand hit should use the diamond sword (" + expectedHit + "), was " + offHit + " (main hand " + mainHit + ")");
        double matchup = DamageStyles.modifier(skeleton, DamageStyles.weapon(diamond));
        double levelBonus = 1 + CombatDamage.LEVEL_DAMAGE_BONUS * (30 - 1);
        double style = player.getData(ModAttachments.ATTACK_STYLE).damageDealtMultiplier();
        double expected = offHit * 1.0 * matchup * levelBonus * style;
        Denied cut = AbilityService.use(player, twinfang);
        helper.assertTrue(cut == null, "Twinfang refused with " + cut + "; " + targetDebug(player, skeleton));
        float dealt = start - skeleton.getHealth();
        helper.assertTrue(Math.abs(dealt - expected) < 0.05, "Twinfang should deal " + expected + " (off hand " + offHit + " x " + DamageStyles.weapon(diamond)
                + " " + matchup + " x level " + levelBonus + " x style " + style + "), dealt " + dealt);

        // Torchbrand: a main-hand strike that sets the target alight (husks don't burn in daylight)
        ServerPlayer torch = player(helper, "skillcraft_ability_torch", new Vec3(4, 1, 3), -90f);
        level(torch, Skill.MELEE, 5);
        hold(torch, new ItemStack(Items.STONE_SWORD));
        Husk husk = helper.spawnWithNoFreeWill(EntityTypes.HUSK, new Vec3(6, 1, 3));
        face(torch, husk);
        Identifier brand = id("torchbrand");
        is(helper, AbilityService.check(torch, brand, AbilityManager.get(brand)), Denied.WEAPON, "Torchbrand without a torch");
        torch.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.TORCH));
        Denied lit = AbilityService.use(torch, brand);
        helper.assertTrue(lit == null, "Torchbrand refused with " + lit + "; " + targetDebug(torch, husk));
        helper.assertTrue(husk.getRemainingFireTicks() > 0, "Torchbrand should set the target on fire");

        SkillCraft.LOGGER.info("[abilities] offhand: Twinfang with an off-hand diamond sword (main hand iron) dealt {} = off-hand hit {} x {} {} x level {} x style {}; Torchbrand fire {} ticks",
                String.format("%.2f", dealt), String.format("%.2f", offHit), DamageStyles.weapon(diamond).getSerializedName(), matchup,
                String.format("%.4f", levelBonus), style, husk.getRemainingFireTicks());
        helper.succeed();
    }

    // --- Bosses: stun -> Daze (once per 20 s, never cancels a windup), capped slows, no knockback ---

    static void bossDaze(GameTestHelper helper) {
        ServerPlayer player = player(helper, "skillcraft_ability_boss", new Vec3(2, 1, 2), 0f);
        SlagheartEntity idle = helper.spawn(ModEntities.SLAGHEART.get(), new Vec3(5, 1, 8));
        SlagheartEntity busy = helper.spawn(ModEntities.SLAGHEART.get(), new Vec3(11, 1, 8));
        idle.setNoAi(true);
        busy.setNoAi(true);
        busy.showcasePose(SlagheartAction.HAMMERFALL);

        int cooldown = idle.attackCooldown();
        SlagheartAction idleAction = idle.action();
        helper.assertTrue(CrowdControl.stun(idle, 30, player), "the first stun on a boss should daze it");
        helper.assertFalse(idle.hasEffect(ModEffects.STUNNED), "bosses must never be stunned");
        helper.assertTrue(idle.hasEffect(ModEffects.DAZED), "a dazed boss should show Dazed");
        if (idleAction == SlagheartAction.NONE) {
            helper.assertValueEqual(idle.attackCooldown(), cooldown + CrowdControl.DAZE_TICKS, "Daze should push the next attack back 1 s");
        }
        helper.assertFalse(CrowdControl.stun(idle, 30, player), "a second Daze within 20 s should be refused");

        int busyCooldown = busy.attackCooldown();
        helper.assertTrue(CrowdControl.stun(busy, 30, player), "a boss mid-attack can still be dazed");
        helper.assertValueEqual(busy.action(), SlagheartAction.HAMMERFALL, "Daze must not cancel an attack in progress");
        helper.assertValueEqual(busy.attackCooldown(), busyCooldown, "mid-attack Daze waits until the attack ends");

        // Roots become a capped slow for half the time; slows are capped at 30%
        CrowdControl.apply(idle, ModEffects.ROOTED, 40, 0, player);
        helper.assertFalse(idle.hasEffect(ModEffects.ROOTED), "bosses must never be rooted");
        MobEffectInstance slow = idle.getEffect(ModEffects.SLOWED);
        helper.assertTrue(slow != null && slow.getAmplifier() == 2 && slow.getDuration() <= 20, "root on a boss -> Slowed III for half the time, got " + slow);
        CrowdControl.apply(busy, ModEffects.SLOWED, 80, 5, player);
        MobEffectInstance capped = busy.getEffect(ModEffects.SLOWED);
        helper.assertTrue(capped != null && capped.getAmplifier() == 2 && capped.getDuration() <= 40, "boss slows cap at 30% for half the time, got " + capped);
        // No knockback
        Vec3 motion = idle.getDeltaMovement();
        CrowdControl.knockback(idle, player, 2.0);
        helper.assertTrue(idle.getDeltaMovement().distanceTo(motion) < 1.0E-6, "bosses must not be knocked back by abilities");
        SkillCraft.LOGGER.info("[abilities] boss rules: stun -> Daze (+{} ticks, 1 per {} ticks), windup kept, root/slow -> Slowed III for half, no knockback",
                CrowdControl.DAZE_TICKS, CrowdControl.DAZE_LOCKOUT);
        helper.succeed();
    }

    // --- Movement stops at walls: Lunge into a wall 1.5 blocks away ---

    static void wall(GameTestHelper helper) {
        for (int y = 1; y <= 3; y++) for (int z = 6; z <= 10; z++) helper.setBlock(new BlockPos(6, y, z), Blocks.STONE);
        ServerPlayer player = player(helper, "skillcraft_ability_wall", new Vec3(4.5, 1, 8.5), -90f); // facing +x, towards the wall
        level(player, Skill.MELEE, 1);
        hold(player, new ItemStack(Items.IRON_SWORD));
        double startX = player.getX();
        ok(helper, AbilityService.use(player, id("lunge")), "Lunge");
        double wallFace = helper.absolutePos(new BlockPos(6, 1, 8)).getX();
        helper.assertTrue(player.getX() > startX, "Lunge should still move you up to the wall");
        helper.assertTrue(player.getX() + player.getBbWidth() / 2 <= wallFace, "Lunge went into the wall: x " + player.getX() + ", wall at " + wallFace);
        SkillCraft.LOGGER.info("[abilities] wall: Lunge from x={} stopped at x={} (wall face {})", String.format("%.2f", startX), String.format("%.2f", player.getX()), wallFace);
        helper.succeed();
    }

    // --- Loadouts: changing the active bar is blocked in combat; locked abilities can't be slotted ---

    static void loadout(GameTestHelper helper) {
        ServerPlayer player = player(helper, "skillcraft_ability_loadout", new Vec3(8, 1, 8), 0f);
        level(player, Skill.MELEE, 1);
        level(player, Skill.DEFENCE, 1);
        Identifier[] bar = {id("lunge"), id("brace"), null, null};
        ok(helper, AbilityService.setLoadout(player, 0, bar, "Main", false), "set the active bar out of combat");
        helper.assertValueEqual(AbilityService.data(player).activeSlot(0), id("lunge"), "slot 1 should hold Lunge");
        is(helper, AbilityService.setLoadout(player, 1, new Identifier[]{id("hamstring")}, "", false), Denied.LOCKED, "a locked ability can't be slotted");

        AbilityService.markCombat(player);
        is(helper, AbilityService.setLoadout(player, 0, new Identifier[]{id("brace")}, null, false), Denied.IN_COMBAT, "editing the active bar in combat");
        is(helper, AbilityService.selectLoadout(player, 1), Denied.IN_COMBAT, "switching bars in combat");
        ok(helper, AbilityService.setLoadout(player, 2, new Identifier[]{id("brace")}, "Spare", false), "editing a bar you aren't using is fine in combat");
        helper.assertValueEqual(AbilityService.data(player).activeLoadout(), 0, "the active bar must not change in combat");

        AbilityData data = AbilityService.data(player);
        data.setCombatUntil(0);
        AbilityService.save(player, data);
        ok(helper, AbilityService.selectLoadout(player, 2), "switching bars out of combat");
        helper.assertValueEqual(AbilityService.data(player).activeSlot(0), id("brace"), "bar 3 is now active");
        helper.succeed();
    }

    // --- Future Spells and Prayers: item-cost abilities and toggles with drain, with no new code ---

    static void future(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = player(helper, "skillcraft_ability_future", new Vec3(8, 1, 8), 0f);
        AbilityDefinition.Requirement anyone = new AbilityDefinition.Requirement(Skill.CONSTITUTION, 1, Optional.empty(), Optional.empty(), Optional.empty(), false);
        Identifier spell = id("test_rune_spell");
        AbilityDefinition spellDef = new AbilityDefinition(AbilityCategory.UTILITY, AbilityClass.BASIC, anyone, 0, AbilityDefinition.Momentum.DEFAULT, true, 0,
                List.of(new AbilityDefinition.ItemCost(Items.REDSTONE, 2)), Optional.empty(), new HealOverTime(0.1f, 20), id("ability/second_wind"), Optional.empty());
        Identifier prayer = id("test_toggle_prayer");
        AbilityDefinition prayerDef = new AbilityDefinition(AbilityCategory.UTILITY, AbilityClass.BASIC, anyone, 0, AbilityDefinition.Momentum.DEFAULT, true, 0,
                List.of(), Optional.of(new AbilityDefinition.Toggle(10f)), new ToggleEffect(ModEffects.FLEET, 0), id("ability/sprint_burst"), Optional.empty());
        // The JSON format round-trips (so these could just as well be data files)
        var json = AbilityDefinition.CODEC.encodeStart(JsonOps.INSTANCE, prayerDef).getOrThrow();
        var again = AbilityDefinition.CODEC.encodeStart(JsonOps.INSTANCE, AbilityDefinition.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow()).getOrThrow();
        helper.assertTrue(json.equals(again), "toggle ability JSON round-trip: " + json + " vs " + again);
        AbilityManager.putForTests(spell, spellDef);
        AbilityManager.putForTests(prayer, prayerDef);

        // Item cost: refused without the items, consumes them when used
        is(helper, AbilityService.use(player, spell), Denied.ITEMS, "rune spell without runes");
        player.getInventory().add(new ItemStack(Items.REDSTONE, 3));
        ok(helper, AbilityService.use(player, spell), "rune spell with runes");
        helper.assertValueEqual(player.getInventory().countItem(Items.REDSTONE), 1, "the spell should consume 2 of 3 runes");

        // Toggle: on, drains 10 Momentum/s, off on a second press, and switches itself off when dry
        setMomentum(player, 30);
        AbilityService.markCombat(player); // so normal out-of-combat decay doesn't mix in
        ok(helper, AbilityService.use(player, prayer), "toggle on");
        helper.assertTrue(player.hasEffect(ModEffects.FLEET), "the toggle's effect should be on");
        helper.onEachTick(() -> AbilityService.tick(player));
        helper.runAfterDelay(20, () -> {
            float left = momentum(player);
            helper.assertTrue(left > 18 && left < 22, "a 10/s toggle should drain ~10 in a second, left " + left);
            ok(helper, AbilityService.use(player, prayer), "toggle off");
            helper.assertFalse(player.hasEffect(ModEffects.FLEET), "turning the toggle off removes its effect");
            ok(helper, AbilityService.use(player, prayer), "toggle on again");
        });
        helper.runAfterDelay(70, () -> {
            helper.assertFalse(AbilityRuntime.of(player).toggles.contains(prayer), "the toggle should switch off when Momentum runs out");
            helper.assertTrue(momentum(player) < 0.5f, "Momentum should be drained, left " + momentum(player));
            SkillCraft.LOGGER.info("[abilities] future: item-cost ability consumed 2 runes; toggle drained 10/s and switched off when dry");
            helper.succeed();
        });
    }

    // --- DPS harness: 60 s on a dummy, auto-attacks only vs. auto-attacks + a scripted rotation (spec 6) ---

    public static final int HARNESS_TICKS = 1200;
    private static final int SWING_TICKS = 13; // a sword's 1.6 attacks/s, rounded to whole ticks

    static void dps(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer auto = player(helper, "skillcraft_dps_auto", new Vec3(2.5, 1, 4.5), -90f);
        ServerPlayer rotation = player(helper, "skillcraft_dps_rotation", new Vec3(2.5, 1, 11.5), -90f);
        Husk[] dummies = {dummy(helper, new Vec3(5.5, 1, 4.5)), dummy(helper, new Vec3(5.5, 1, 11.5))};
        ServerPlayer[] players = {auto, rotation};
        for (ServerPlayer player : players) {
            level(player, Skill.MELEE, 40);
            hold(player, new ItemStack(Items.DIAMOND_SWORD));
        }
        face(auto, dummies[0]);
        face(rotation, dummies[1]);
        Identifier[] priority = {id("battle_trance"), id("sweeping_arc"), id("rending_cut"), id("lunge")};
        double[] dealt = new double[2];
        int[] uses = new int[priority.length];
        int[] tick = {0};

        helper.onEachTick(() -> {
            int t = tick[0]++;
            if (t >= HARNESS_TICKS) return;
            for (int i = 0; i < 2; i++) {
                Husk dummy = dummies[i];
                ServerPlayer player = players[i];
                face(player, dummy);
                if (t % SWING_TICKS == 0) {
                    dummy.invulnerableTime = 0;
                    dummy.hurtServer(level, level.damageSources().playerAttack(player), (float) com.toddlandis.skillcraft.ability.AbilityDamage.weaponHit(player));
                }
                if (i == 1) {
                    for (int a = 0; a < priority.length; a++) {
                        if (AbilityService.check(player, priority[a], AbilityManager.get(priority[a])) == null && AbilityService.use(player, priority[a]) == null) {
                            uses[a]++;
                            break;
                        }
                    }
                }
                AbilityService.tick(player);
                tickEffects(level, player);
                dummy.setDeltaMovement(Vec3.ZERO);
                // Keep the dummy topped up and count the damage
                dealt[i] += dummy.getMaxHealth() - dummy.getHealth();
                dummy.setHealth(dummy.getMaxHealth());
                if (player.distanceTo(dummy) < 1.5) player.snapTo(dummy.getX() - 2.0, player.getY(), dummy.getZ(), player.getYRot(), 0f); // Lunge closes in; step back to swing range
            }
        });
        helper.runAfterDelay(HARNESS_TICKS + 30, () -> {
            for (Husk dummy : dummies) {
                // Bleed ticks that landed after the last measurement
                for (int i = 0; i < 2; i++) if (dummies[i] == dummy) dealt[i] += dummy.getMaxHealth() - dummy.getHealth();
            }
            double seconds = HARNESS_TICKS / 20.0;
            double ratio = dealt[1] / dealt[0];
            SkillCraft.LOGGER.info("[dps] {} s on a husk, diamond sword, Melee 40, a swing every {} ticks", seconds, SWING_TICKS);
            SkillCraft.LOGGER.info("[dps] auto-attacks only: {} damage = {} DPS", String.format("%.1f", dealt[0]), String.format("%.2f", dealt[0] / seconds));
            SkillCraft.LOGGER.info("[dps] with rotation:     {} damage = {} DPS (Battle Trance x{}, Sweeping Arc x{}, Rending Cut x{}, Lunge x{})",
                    String.format("%.1f", dealt[1]), String.format("%.2f", dealt[1] / seconds), uses[0], uses[1], uses[2], uses[3]);
            SkillCraft.LOGGER.info("[dps] ratio: {} (first-build target 1.15-1.25; ~1.39 after the playtest pacing pass)", String.format("%.3f", ratio));
            helper.assertTrue(dealt[0] > 0, "the auto-attack player dealt no damage");
            if (ratio < 1.05) SkillCraft.LOGGER.info("[dps] targeting: {}", targetDebug(rotation, dummies[1]));
            helper.assertTrue(ratio >= 1.05 && ratio <= 1.55, "rotation DPS gain out of bounds: x" + String.format("%.3f", ratio));
            helper.succeed();
        });
    }

    private static Husk dummy(GameTestHelper helper, Vec3 pos) {
        Husk husk = helper.spawnWithNoFreeWill(EntityTypes.HUSK, pos);
        husk.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
        husk.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1.0); // a training dummy stays put
        husk.setHealth(husk.getMaxHealth());
        husk.setPersistenceRequired();
        return husk;
    }
}
