package com.toddlandis.skillcraft.gametest;

import com.toddlandis.skillcraft.SkillCraft;
import com.toddlandis.skillcraft.ability.AbilityDefinition;
import com.toddlandis.skillcraft.ability.AbilityRuntime;
import com.toddlandis.skillcraft.ability.AbilityService;
import com.toddlandis.skillcraft.ability.AbilityService.Denied;
import com.toddlandis.skillcraft.agility.AgilityTracker;
import com.toddlandis.skillcraft.combat.CombatDamage;
import com.toddlandis.skillcraft.combat.DamageStyle;
import com.toddlandis.skillcraft.combat.DamageStyles;
import com.toddlandis.skillcraft.magic.MagicCasting;
import com.toddlandis.skillcraft.prayer.PlayerFaith;
import com.toddlandis.skillcraft.prayer.PrayerEvents;
import com.toddlandis.skillcraft.prayer.PrayerService;
import com.toddlandis.skillcraft.registry.ModAttachments;
import com.toddlandis.skillcraft.registry.ModEffects;
import com.toddlandis.skillcraft.registry.ModItems;
import com.toddlandis.skillcraft.skill.Skill;
import com.toddlandis.skillcraft.skill.SkillManager;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;

// GameTests for Phase 7 (docs/AGILITY_MAGIC_PRAYER.md): magic bolts and spells through the combat
// pipeline (Magic XP, elements, staves supplying motes), blessings (Faith drain, exclusive groups,
// wards, boosts, Saving Grace, offerings) and Agility (parkour XP with the novelty rule, safe falls,
// Grapple Line and Shadowstep stopping where they should).
public final class Phase7Tests {
    private Phase7Tests() {}

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(SkillCraft.MODID, path);
    }

    private static ServerPlayer player(GameTestHelper helper, String name, Vec3 pos, float yaw) {
        ServerPlayer player = AbilityTests.player(helper, name, pos, yaw);
        player.getInventory().clearContent();
        player.setData(ModAttachments.FAITH, new PlayerFaith());
        return player;
    }

    private static double levelBonus(int level) {
        return 1 + CombatDamage.LEVEL_DAMAGE_BONUS * (level - 1);
    }

    private static double styleBonus(ServerPlayer player) {
        return player.getData(ModAttachments.ATTACK_STYLE).damageDealtMultiplier();
    }

    private static int count(ServerPlayer player, net.minecraft.world.item.Item item) {
        return AbilityService.countItem(player, item);
    }

    // --- Bolts: the wand costs an arcane mote, an elemental staff supplies its own; Magic XP per damage ---

    static void bolt(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer wand = player(helper, "skillcraft_magic_wand", new Vec3(2, 1, 4), -90f);
        AbilityTests.level(wand, Skill.MAGIC, 1);
        AbilityTests.hold(wand, new ItemStack(ModItems.APPRENTICE_WAND.get()));
        wand.getInventory().add(new ItemStack(ModItems.ARCANE_MOTE.get(), 1));
        LivingEntity pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new Vec3(7, 1, 4));
        AbilityTests.face(wand, pig);

        ServerPlayer staff = player(helper, "skillcraft_magic_staff", new Vec3(2, 1, 12), -90f);
        AbilityTests.level(staff, Skill.MAGIC, 10);
        AbilityTests.hold(staff, new ItemStack(ModItems.EMBER_STAFF.get()));
        LivingEntity pig2 = helper.spawnWithNoFreeWill(EntityTypes.PIG, new Vec3(7, 1, 12));
        AbilityTests.face(staff, pig2);

        // A focus above your level won't cast
        ServerPlayer novice = player(helper, "skillcraft_magic_novice", new Vec3(2, 1, 8), -90f);
        helper.assertFalse(MagicCasting.castBolt(novice, new ItemStack(ModItems.STARSEER_STAFF.get())), "a Magic 1 player must not cast with a Magic 40 staff");

        float pigBefore = pig.getHealth(), pig2Before = pig2.getHealth();
        double xpBefore = SkillManager.get(wand).getXp(Skill.MAGIC), rangedBefore = SkillManager.get(wand).getXp(Skill.RANGED);
        helper.assertTrue(MagicCasting.castBolt(wand, wand.getMainHandItem()), "the wand should fire with an arcane mote");
        helper.assertTrue(count(wand, ModItems.ARCANE_MOTE.get()) == 0, "the bolt should cost the arcane mote");
        helper.assertFalse(MagicCasting.castBolt(wand, wand.getMainHandItem()), "with no motes left the wand must not fire");
        helper.assertTrue(MagicCasting.castBolt(staff, staff.getMainHandItem()), "the Ember Staff should fire with no Ember Motes (it supplies them)");

        double expectedWand = 4.0 * DamageStyles.modifier(pig, DamageStyle.ARCANE) * levelBonus(1) * styleBonus(wand);
        double expectedStaff = 8.0 * DamageStyles.modifier(pig2, DamageStyle.FIRE) * levelBonus(10) * styleBonus(staff);
        helper.runAfterDelay(20, () -> {
            float dealt = pigBefore - pig.getHealth(), dealt2 = pig2Before - pig2.getHealth();
            double xp = SkillManager.get(wand).getXp(Skill.MAGIC) - xpBefore;
            helper.assertTrue(Math.abs(dealt - expectedWand) < 0.05, "wand bolt should deal " + expectedWand + ", dealt " + dealt);
            helper.assertTrue(Math.abs(dealt2 - expectedStaff) < 0.05, "staff bolt should deal " + expectedStaff + ", dealt " + dealt2);
            helper.assertTrue(Math.abs(xp - dealt * 4.0) < 0.05, "a bolt should pay 4 Magic XP per damage, got " + xp + " for " + dealt);
            helper.assertTrue(SkillManager.get(wand).getXp(Skill.RANGED) == rangedBefore, "magic damage must not pay Ranged XP");
            SkillCraft.LOGGER.info("[magic] bolts: Apprentice Wand {} (arcane, Magic 1) = 4.00 x {}; Ember Staff {} (fire, Magic 10, no motes needed) = 8.00 x {}; {} Magic XP",
                    String.format("%.2f", dealt), String.format("%.4f", levelBonus(1) * styleBonus(wand)), String.format("%.2f", dealt2),
                    String.format("%.4f", levelBonus(10) * styleBonus(staff)), String.format("%.1f", xp));
            helper.succeed();
        });
    }

    // --- Spells: Frost Nova (an area spell scaling with the staff's power) and Chain Lightning's jumps ---

    static void spells(GameTestHelper helper) {
        ServerPlayer frost = player(helper, "skillcraft_magic_nova", new Vec3(4, 1, 4), 0f);
        AbilityTests.level(frost, Skill.MAGIC, 28);
        AbilityTests.hold(frost, new ItemStack(ModItems.RIME_STAFF.get()));
        frost.getInventory().add(new ItemStack(ModItems.ARCANE_MOTE.get(), 1));
        AbilityTests.setMomentum(frost, 100);
        LivingEntity a = helper.spawnWithNoFreeWill(EntityTypes.IRON_GOLEM, new Vec3(7, 1, 4));
        LivingEntity b = helper.spawnWithNoFreeWill(EntityTypes.IRON_GOLEM, new Vec3(4, 1, 7));
        float aBefore = a.getHealth(), bBefore = b.getHealth();

        ServerPlayer storm = player(helper, "skillcraft_magic_chain", new Vec3(1.5, 1, 11.5), -90f);
        AbilityTests.level(storm, Skill.MAGIC, 22);
        AbilityTests.hold(storm, new ItemStack(ModItems.TEMPEST_STAFF.get()));
        storm.getInventory().add(new ItemStack(ModItems.ARCANE_MOTE.get(), 1));
        AbilityTests.setMomentum(storm, 100);
        LivingEntity[] golems = {
                helper.spawnWithNoFreeWill(EntityTypes.IRON_GOLEM, new Vec3(5.5, 1, 11.5)),
                helper.spawnWithNoFreeWill(EntityTypes.IRON_GOLEM, new Vec3(9, 1, 11.5)),
                helper.spawnWithNoFreeWill(EntityTypes.IRON_GOLEM, new Vec3(12.5, 1, 11.5))};
        float[] golemBefore = new float[3];
        for (int i = 0; i < 3; i++) golemBefore[i] = golems[i].getHealth();
        AbilityTests.face(storm, golems[0]);

        AbilityTests.ok(helper, AbilityService.use(frost, id("frost_nova")), "Frost Nova (it winds up)");
        AbilityTests.ok(helper, AbilityService.use(storm, id("chain_lightning")), "Chain Lightning");
        helper.assertTrue(count(storm, ModItems.ARCANE_MOTE.get()) == 0, "Chain Lightning should spend its arcane mote (storm motes come from the staff)");
        helper.onEachTick(() -> AbilityService.tick(frost));

        double nova = 8.0 * 1.2 * levelBonus(28) * styleBonus(frost);
        double chain = 8.5 * 1.5 * levelBonus(22) * styleBonus(storm);
        helper.runAfterDelay(25, () -> {
            float dealtA = aBefore - a.getHealth(), dealtB = bBefore - b.getHealth();
            helper.assertTrue(Math.abs(dealtA - nova * DamageStyles.modifier(a, DamageStyle.FROST)) < 0.05 && Math.abs(dealtB - nova * DamageStyles.modifier(b, DamageStyle.FROST)) < 0.05,
                    "Frost Nova should deal " + nova + " (before matchups) to both golems, dealt " + dealtA + " and " + dealtB);
            helper.assertTrue(a.hasEffect(ModEffects.SLOWED) && b.hasEffect(ModEffects.SLOWED), "Frost Nova should slow what it hits");
            helper.assertTrue(count(frost, ModItems.ARCANE_MOTE.get()) == 0, "Frost Nova should spend its arcane mote");
            double matchup = DamageStyles.modifier(golems[0], DamageStyle.STORM);
            StringBuilder dealt = new StringBuilder();
            for (int i = 0; i < 3; i++) {
                float d = golemBefore[i] - golems[i].getHealth();
                double expected = chain * matchup * Math.pow(0.75, i);
                helper.assertTrue(Math.abs(d - expected) < 0.06, "Chain Lightning jump " + i + " should deal " + expected + ", dealt " + d);
                dealt.append(String.format(i == 0 ? "%.2f" : " -> %.2f", d));
            }
            SkillCraft.LOGGER.info("[magic] spells: Frost Nova {} each (Rime Staff 8 x 1.2 x level {}, frost x0.8 on golems), Chain Lightning {} (Tempest Staff 8.5 x 1.5, storm x{} on golems, -25% per jump)",
                    String.format("%.2f", dealtA), String.format("%.4f", levelBonus(28)), dealt, matchup);
            helper.succeed();
        });
    }

    // --- Prayer: Faith drain, exclusive groups, wards and boosts, running dry, offerings, Saving Grace ---

    static void prayer(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = player(helper, "skillcraft_prayer", new Vec3(4, 1, 4), 0f);
        AbilityTests.level(player, Skill.PRAYER, 45);
        float max = PrayerService.maxFaith(player);
        helper.assertTrue(max == 9 + 45, "maximum Faith at Prayer 45 should be 54, was " + max);
        helper.assertTrue(PrayerService.points(player) == max, "a new player should start with full Faith");
        Zombie zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new Vec3(6, 1, 4));
        AbilityRuntime runtime = AbilityRuntime.of(player);

        // Exclusive groups: one ward at a time
        AbilityTests.ok(helper, AbilityService.useFromBook(player, id("ward_of_arrows")), "Ward of Arrows");
        AbilityTests.ok(helper, AbilityService.useFromBook(player, id("ward_of_steel")), "Ward of Steel");
        helper.assertTrue(runtime.toggles.contains(id("ward_of_steel")) && !runtime.toggles.contains(id("ward_of_arrows")),
                "turning on Ward of Steel should turn off Ward of Arrows: " + runtime.toggles);
        AbilityTests.is(helper, AbilityService.useFromBook(player, id("lunge")), Denied.INVALID, "the book can only toggle blessings");
        AbilityTests.ok(helper, AbilityService.useFromBook(player, id("battle_fervor")), "Battle Fervor");

        double ward = PrayerEvents.damageMultiplier(player, zombie, DamageStyle.CRUSH);
        double unwarded = PrayerEvents.damageMultiplier(player, zombie, DamageStyle.MEDIUM);
        double boost = PrayerEvents.damageMultiplier(zombie, player, DamageStyle.SLASH);
        double noBoost = PrayerEvents.damageMultiplier(zombie, player, DamageStyle.FIRE);
        helper.assertTrue(Math.abs(ward - 0.6) < 1.0E-6, "Ward of Steel should take 40% off a crush hit, multiplier " + ward);
        helper.assertTrue(unwarded == 1.0, "Ward of Steel must not touch ranged hits, multiplier " + unwarded);
        helper.assertTrue(Math.abs(boost - 1.1) < 1.0E-6, "Battle Fervor should add 10% to a melee hit, multiplier " + boost);
        helper.assertTrue(noBoost == 1.0, "Battle Fervor must not boost spells, multiplier " + noBoost);

        // Offerings: burying pays the base XP, a shrine three times as much
        ItemStack bones = new ItemStack(Items.BONE, 10);
        double xpBefore = SkillManager.get(player).getXp(Skill.PRAYER);
        helper.assertTrue(PrayerService.offer(player, bones, bones.getCount(), true) == 10, "the shrine should take all 10 bones");
        double offered = SkillManager.get(player).getXp(Skill.PRAYER) - xpBefore;
        ItemStack bone = new ItemStack(Items.BONE, 1);
        PrayerService.offer(player, bone, 1, false);
        double buried = SkillManager.get(player).getXp(Skill.PRAYER) - xpBefore - offered;
        helper.assertTrue(Math.abs(offered - 150) < 1.0E-6 && Math.abs(buried - 5) < 1.0E-6, "10 bones at a shrine = 150 XP, one buried = 5; got " + offered + " and " + buried);
        helper.assertTrue(bones.isEmpty() && bone.isEmpty(), "offerings should be used up");

        // Saving Grace: a killing blow leaves you at 25% health and spends all Faith
        AbilityTests.ok(helper, AbilityService.useFromBook(player, id("saving_grace")), "Saving Grace");
        player.setHealth(4f);
        LivingDamageEvent.Pre lethal = new LivingDamageEvent.Pre(player, new DamageContainer(level.damageSources().mobAttack(zombie), 30f));
        NeoForge.EVENT_BUS.post(lethal);
        helper.assertTrue(lethal.getNewDamage() == 0f, "Saving Grace should cancel the killing blow, damage " + lethal.getNewDamage());
        helper.assertTrue(player.getHealth() >= player.getMaxHealth() * 0.25f - 1.0E-4f, "Saving Grace should leave 25% health, health " + player.getHealth());
        helper.assertTrue(PrayerService.points(player) == 0, "Saving Grace should spend all Faith");

        // Running dry: the next tick switches every blessing off; nothing turns on at 0 Faith
        AbilityService.tick(player);
        helper.assertTrue(runtime.toggles.isEmpty(), "blessings should switch off when Faith runs out: " + runtime.toggles);
        AbilityTests.is(helper, AbilityService.useFromBook(player, id("ironhide")), Denied.FAITH, "a blessing with no Faith");

        // Drain: Ward of Steel + Battle Fervor take 0.35 Faith a second
        PrayerService.restore(player);
        runtime.readyAt.clear();
        AbilityTests.ok(helper, AbilityService.useFromBook(player, id("ward_of_steel")), "Ward of Steel again");
        AbilityTests.ok(helper, AbilityService.useFromBook(player, id("battle_fervor")), "Battle Fervor again");
        float start = PrayerService.points(player);
        for (int t = 0; t < 40; t++) AbilityService.tick(player);
        float drained = start - PrayerService.points(player);
        helper.assertTrue(Math.abs(drained - 0.7f) < 0.02f, "two blessings should drain 0.70 Faith in 2 s, drained " + drained);
        SkillCraft.LOGGER.info("[prayer] Faith max {} at Prayer 45; Ward of Steel x{} vs crush, Battle Fervor x{} on melee; drain {} in 2 s; 10 bones offered {} XP, 1 buried {} XP; Saving Grace left {} health",
                max, String.format("%.2f", ward), String.format("%.2f", boost), String.format("%.2f", drained), offered, buried, player.getMaxHealth() * 0.25f);
        helper.succeed();
    }

    // --- Agility: gap jump and climb XP (once per spot), safe falls, Grapple Line and Shadowstep ---

    static void agility(GameTestHelper helper) {
        ServerPlayer player = player(helper, "skillcraft_agility", new Vec3(3.5, 4, 8.5), -90f);
        AbilityTests.level(player, Skill.AGILITY, 40);
        // Two 3-block pillars with a gap between them
        for (int y = 1; y <= 3; y++) {
            helper.setBlock(new BlockPos(3, y, 8), Blocks.STONE);
            helper.setBlock(new BlockPos(7, y, 8), Blocks.STONE);
        }
        Vec3 takeoff = helper.absoluteVec(new Vec3(3.5, 4, 8.5)), air = helper.absoluteVec(new Vec3(5.5, 5, 8.5)), landing = helper.absoluteVec(new Vec3(7.5, 4, 8.5));
        double xp0 = SkillManager.get(player).getXp(Skill.AGILITY);
        jump(player, takeoff, air, landing);
        double gap = SkillManager.get(player).getXp(Skill.AGILITY) - xp0;
        helper.assertTrue(Math.abs(gap - 8) < 1.0E-6, "a 4-block sprint jump over a gap should pay 8 Agility XP, paid " + gap);
        jump(player, takeoff, air, landing);
        helper.assertTrue(SkillManager.get(player).getXp(Skill.AGILITY) - xp0 == gap, "the same jump again within a minute must not pay");

        // Climb a ladder 5 blocks up
        for (int y = 1; y <= 7; y++) {
            helper.setBlock(new BlockPos(12, y, 3), Blocks.STONE);
            helper.setBlock(new BlockPos(11, y, 3), Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.WEST));
        }
        double xp1 = SkillManager.get(player).getXp(Skill.AGILITY);
        player.setSprinting(false);
        for (int y = 1; y <= 6; y++) step(player, helper.absoluteVec(new Vec3(11.5, y, 3.5)), false);
        step(player, helper.absoluteVec(new Vec3(10.5, 6, 3.5)), true);
        double climb = SkillManager.get(player).getXp(Skill.AGILITY) - xp1;
        helper.assertTrue(Math.abs(climb - 5) < 1.0E-6, "climbing 5 blocks should pay 5 Agility XP, paid " + climb);

        // Sure footing: Agility 40 makes the first 4 blocks of a fall harmless
        LivingFallEvent fall = new LivingFallEvent(player, 7.0, 1.0f);
        NeoForge.EVENT_BUS.post(fall);
        helper.assertTrue(Math.abs(fall.getDistance() - 3.0) < 1.0E-6, "a 7-block fall at Agility 40 should count as 3, was " + fall.getDistance());

        // Grapple Line to a wall: you end up in front of it, never inside or past it
        ServerPlayer grappler = player(helper, "skillcraft_grapple", new Vec3(2.5, 1, 13.5), -90f);
        AbilityTests.level(grappler, Skill.AGILITY, 40);
        for (int y = 1; y <= 4; y++) for (int z = 12; z <= 15; z++) helper.setBlock(new BlockPos(14, y, z), Blocks.STONE);
        AbilityTests.ok(helper, AbilityService.use(grappler, id("grapple_line")), "Grapple Line at a wall");
        double wallFace = helper.absoluteVec(new Vec3(14, 0, 0)).x;
        double reach = grappler.getX() + grappler.getBbWidth() / 2;
        helper.assertTrue(reach <= wallFace + 1.0E-3 && grappler.getX() > helper.absoluteVec(new Vec3(9, 0, 0)).x,
                "Grapple Line should pull you up to the wall without entering it: x " + grappler.getX() + ", wall face " + wallFace);

        // Shadowstep: behind the zombie, facing it, and the next melee hit is boosted
        ServerPlayer rogue = player(helper, "skillcraft_shadowstep", new Vec3(8.5, 1, 1.5), 0f);
        AbilityTests.level(rogue, Skill.AGILITY, 40);
        AbilityTests.setMomentum(rogue, 100);
        Zombie zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new Vec3(8.5, 1, 5.5));
        zombie.setYRot(180f);
        zombie.setYHeadRot(180f);
        zombie.setYBodyRot(180f);
        AbilityTests.face(rogue, zombie);
        AbilityTests.ok(helper, AbilityService.use(rogue, id("shadowstep")), "Shadowstep");
        helper.assertTrue(rogue.getZ() > zombie.getZ() + 0.5, "Shadowstep should put you behind the zombie: you z " + rogue.getZ() + ", zombie z " + zombie.getZ());
        helper.assertTrue(AbilityRuntime.of(rogue).counterBonus == 0.25f && AbilityRuntime.of(rogue).counterUntil > helper.getLevel().getGameTime(),
                "Shadowstep should boost the next melee hit");
        SkillCraft.LOGGER.info("[agility] gap jump {} XP (once per spot), climb 5 blocks {} XP, 7-block fall at Agility 40 counts as {}; Grapple stopped at x {} (wall {}); Shadowstep moved z {} -> {}",
                gap, climb, fall.getDistance(), String.format("%.2f", reach), wallFace, String.format("%.2f", helper.absoluteVec(new Vec3(8.5, 1, 1.5)).z), String.format("%.2f", rogue.getZ()));
        helper.succeed();
    }

    // Takeoff (on the ground), mid-air (sprinting), landing: what the tracker sees from a real sprint jump
    private static void jump(ServerPlayer player, Vec3 takeoff, Vec3 air, Vec3 landing) {
        step(player, takeoff, true);
        player.setSprinting(true);
        step(player, air, false);
        step(player, landing, true);
        player.setSprinting(false);
    }

    private static void step(ServerPlayer player, Vec3 pos, boolean onGround) {
        player.setPos(pos.x, pos.y, pos.z);
        player.setOnGround(onGround);
        AgilityTracker.tick(player);
    }
}
