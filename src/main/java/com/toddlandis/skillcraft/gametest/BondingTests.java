package com.toddlandis.skillcraft.gametest;

import java.util.UUID;

import com.toddlandis.skillcraft.SkillCraft;
import com.toddlandis.skillcraft.bonding.BondCodex;
import com.toddlandis.skillcraft.bonding.BondService;
import com.toddlandis.skillcraft.bonding.Cliffstrider;
import com.toddlandis.skillcraft.bonding.Cloudray;
import com.toddlandis.skillcraft.bonding.FetchItemsGoal;
import com.toddlandis.skillcraft.bonding.FollowBonderGoal;
import com.toddlandis.skillcraft.registry.ModAttachments;
import com.toddlandis.skillcraft.registry.ModBlocks;
import com.toddlandis.skillcraft.registry.ModEntities;
import com.toddlandis.skillcraft.registry.ModItems;
import com.toddlandis.skillcraft.skill.Skill;
import com.toddlandis.skillcraft.skill.SkillManager;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

// GameTests for Beastbonding (docs/BEASTBONDING.md): bonding rules, the summon/store/release round
// trip keeping a creature's name and health, changed creatures (Cliffstrider, fox helpers), level
// bonuses, death and rekindling at a shrine, and putting stray "Out" bonds back on login.
// Fake players aren't in the server's player list, so owner lookups by id find nobody; these tests
// call BondService directly where the live game would go through an event.
public final class BondingTests {
    private BondingTests() {}

    private static ServerPlayer player(GameTestHelper helper, String name, int level) {
        ServerPlayer player = AbilityTests.player(helper, name, new Vec3(8, 1, 4), 0f);
        player.setData(ModAttachments.BOND_CODEX, new BondCodex());
        player.getInventory().clearContent();
        AbilityTests.level(player, Skill.BEASTBONDING, level);
        BondService.resetSummonCooldown(player);
        return player;
    }

    private static <T extends Entity> T spawn(GameTestHelper helper, net.minecraft.world.entity.EntityType<T> type, int x, int z) {
        return helper.spawn(type, new BlockPos(x, 1, z));
    }

    // --- Bonding, then summon, store and release ---
    static void roundTrip(GameTestHelper helper) {
        ServerPlayer player = player(helper, "skillcraft_bonder", 10);
        var wolf = spawn(helper, EntityTypes.WOLF, 8, 8);

        helper.assertFalse(BondService.bond(player, wolf), "an untamed wolf must not bond");
        wolf.tame(player);
        wolf.setCustomName(Component.literal("Rex"));
        wolf.setHealth(5f);
        double xpBefore = SkillManager.get(player).getXp(Skill.BEASTBONDING);
        helper.assertTrue(BondService.bond(player, wolf), "a wolf you tamed should bond");
        helper.assertTrue(wolf.isRemoved(), "a bonded creature leaves the world");
        BondCodex codex = BondService.codex(player);
        BondCodex.Bond bond = codex.bonds().getFirst();
        helper.assertTrue(codex.bonds().size() == 1 && bond.state == BondCodex.State.STORED && codex.pages().size() == 1, "the codex should hold one stored wolf and one page");
        double xp = SkillManager.get(player).getXp(Skill.BEASTBONDING) - xpBefore;
        helper.assertTrue(Math.abs(xp - 100) < 0.01, "a new page pays 5 x 20 = 100 XP, paid " + xp);

        Entity summoned = BondService.summon(player, bond.id);
        helper.assertTrue(summoned instanceof net.minecraft.world.entity.animal.wolf.Wolf w && "Rex".equals(w.getCustomName().getString())
                        && Math.abs(w.getHealth() - 5f) < 0.6f && w.isOwnedBy(player),
                "the summoned wolf should be Rex, still hurt and still yours (" + summoned + ")");
        helper.assertTrue(bond.state == BondCodex.State.OUT && player.getUUID().equals(BondService.ownerOf(summoned)), "a summon is tagged as yours and Out");

        ((LivingEntity) summoned).setHealth(3f);
        helper.assertTrue(BondService.store(player, bond) && summoned.isRemoved() && bond.state == BondCodex.State.STORED && Math.abs(bond.health - 3f) < 0.01,
                "storing saves the creature as it is now (health " + bond.health + ")");

        helper.assertTrue(BondService.release(player, bond.id) && codex.bonds().isEmpty(), "releasing removes the bond");
        boolean freed = !helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.animal.wolf.Wolf.class, player.getBoundingBox().inflate(6),
                w -> "Rex".equals(w.getCustomName() == null ? "" : w.getCustomName().getString()) && !BondService.isBonded(w)).isEmpty();
        helper.assertTrue(freed, "a released wolf appears beside you, no longer bonded");
        SkillCraft.LOGGER.info("[bonding] wolf: bonded (+{} XP, new page), summoned at 5 HP, stored at 3 HP, released", xp);
        helper.succeed();
    }

    // --- Offerings, levels, slots, changed creatures and limits ---
    static void rules(GameTestHelper helper) {
        ServerPlayer player = player(helper, "skillcraft_rules", 15);

        // Fox: an offering of 3 sweet berries; then it follows and fetches
        Fox fox = spawn(helper, EntityTypes.FOX, 4, 8);
        helper.assertFalse(BondService.bond(player, fox), "a fox needs its offering");
        player.getInventory().add(new ItemStack(Items.SWEET_BERRIES, 5));
        helper.assertTrue(BondService.bond(player, fox) && AbilityTests.countOf(player, Items.SWEET_BERRIES) == 2, "bonding a fox takes 3 of 5 berries");

        // Spider: Beastbonding 35, and it comes back as a Cliffstrider
        var spider = spawn(helper, EntityTypes.SPIDER, 12, 8);
        player.getInventory().add(new ItemStack(Items.STRING, 8));
        helper.assertFalse(BondService.bond(player, spider), "a spider needs Beastbonding 35");
        AbilityTests.level(player, Skill.BEASTBONDING, 40);
        helper.assertTrue(BondService.bond(player, spider), "at 40 the spider bonds");
        BondCodex codex = BondService.codex(player);
        BondCodex.Bond foxBond = codex.bonds().get(0);
        BondCodex.Bond spiderBond = codex.bonds().get(1);

        Entity cliff = BondService.summon(player, spiderBond.id);
        helper.assertTrue(cliff instanceof Cliffstrider c && c.targetSelector.getAvailableGoals().isEmpty(),
                "a bonded spider is summoned as a Cliffstrider with no hunting goals (" + cliff + ")");
        double speedBonus = ((LivingEntity) cliff).getAttribute(Attributes.MOVEMENT_SPEED).getModifier(
                net.minecraft.resources.Identifier.fromNamespaceAndPath(SkillCraft.MODID, "bond_bonus")).amount();
        helper.assertTrue(Math.abs(speedBonus - 0.1) < 1e-9, "a mount at Beastbonding 40 gets +10% speed, got " + speedBonus);

        BondService.resetSummonCooldown(player);
        Entity foxOut = BondService.summon(player, foxBond.id);
        helper.assertTrue(foxOut instanceof Fox f && f.goalSelector.getAvailableGoals().stream().anyMatch(g -> g.getGoal() instanceof FollowBonderGoal)
                        && f.goalSelector.getAvailableGoals().stream().anyMatch(g -> g.getGoal() instanceof FetchItemsGoal),
                "a bonded fox follows and fetches");

        // One companion out below level 50: a second companion sends the first home
        var wolf = spawn(helper, EntityTypes.WOLF, 8, 12);
        wolf.tame(player);
        BondService.bond(player, wolf);
        BondCodex.Bond wolfBond = codex.bonds().get(2);
        BondService.resetSummonCooldown(player);
        BondService.summon(player, wolfBond.id);
        helper.assertTrue(wolfBond.state == BondCodex.State.OUT && foxBond.state == BondCodex.State.STORED && spiderBond.state == BondCodex.State.OUT,
                "a second companion stores the first; the mount stays out");

        // Fetching: an item near you goes straight into your inventory
        ItemEntity apple = new ItemEntity(helper.getLevel(), player.getX() + 2, player.getY(), player.getZ(), new ItemStack(Items.APPLE, 3));
        helper.getLevel().addFreshEntity(apple);
        helper.assertTrue(FetchItemsGoal.fetch(player, apple) && apple.isRemoved() && AbilityTests.countOf(player, Items.APPLE) == 3, "a fetched stack lands in your inventory");

        // Slots: 4 + level / 5
        helper.assertTrue(BondService.slots(1) == 4 && BondService.slots(40) == 12 && BondService.slots(99) == 23, "bond slots should be 4, 12 and 23 at 1, 40 and 99");
        helper.assertTrue(BondService.companionLimit(49) == 1 && BondService.companionLimit(50) == 2, "a second companion slot at 50");
        SkillCraft.LOGGER.info("[bonding] rules: fox offering, spider -> Cliffstrider (+{} speed at 40), companion swap, fetch, slots 4/12/23", speedBonus);
        helper.succeed();
    }

    // --- Death, the Rekindling Shrine, and stray Out bonds ---
    static void rekindle(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = player(helper, "skillcraft_rekindler", 10);
        var wolf = spawn(helper, EntityTypes.WOLF, 8, 8);
        wolf.tame(player);
        BondService.bond(player, wolf);
        BondCodex codex = BondService.codex(player);
        BondCodex.Bond bond = codex.bonds().getFirst();
        LivingEntity out = (LivingEntity) BondService.summon(player, bond.id);

        // It dies: Fallen (the death event does this in game; fake players aren't in the player list)
        BondService.fallen(player, out);
        out.discard();
        helper.assertTrue(bond.state == BondCodex.State.FALLEN, "a summon that dies becomes Fallen");

        helper.assertFalse(BondService.rekindle(player, bond.id), "rekindling needs a shrine nearby");
        helper.setBlock(new BlockPos(9, 1, 4), ModBlocks.REKINDLING_SHRINE.get());
        helper.assertFalse(BondService.rekindle(player, bond.id), "rekindling a wolf needs its tribute of 4 bones");
        player.getInventory().add(new ItemStack(Items.BONE, 4));
        helper.assertTrue(BondService.rekindle(player, bond.id) && bond.state == BondCodex.State.REKINDLING && AbilityTests.countOf(player, Items.BONE) == 0,
                "with 4 bones at a shrine the wolf starts rekindling");
        long left = bond.readyAt - level.getServer().overworld().getGameTime();
        helper.assertTrue(left > 1100 && left <= 1200, "a wolf takes 60 s to rekindle, " + left + " ticks left");
        BondService.tick(player);
        helper.assertTrue(bond.state == BondCodex.State.REKINDLING, "not back before its time");
        bond.readyAt = level.getServer().overworld().getGameTime();
        BondService.tick(player);
        helper.assertTrue(bond.state == BondCodex.State.STORED && bond.health == bond.maxHealth && bond.data.getFloatOr("Health", 0f) == bond.maxHealth,
                "after its time it's back at full health (" + bond.health + "/" + bond.maxHealth + ")");

        // A bond that says Out with nothing in the world (after a crash) goes back in on login
        bond.state = BondCodex.State.OUT;
        bond.entity = UUID.randomUUID();
        NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedInEvent(player));
        helper.assertTrue(bond.state == BondCodex.State.STORED && bond.entity == null, "a stray Out bond is put back on login");
        SkillCraft.LOGGER.info("[bonding] rekindle: Fallen -> shrine + 4 bones -> {} ticks -> Stored at {} HP; stray Out fixed on login", left, bond.health);
        helper.succeed();
    }

    // --- The Cloudray: hovers without gravity, the lure needs height, bonding needs 62 and Storm Motes ---
    static void cloudray(GameTestHelper helper) {
        ServerPlayer player = player(helper, "skillcraft_skyrider", 61);
        Cloudray ray = spawn(helper, ModEntities.CLOUDRAY.get(), 8, 10);
        ray.setPos(ray.getX(), ray.getY() + 3, ray.getZ());
        helper.assertTrue(ray.isNoGravity(), "a Cloudray flies without gravity");
        helper.assertFalse(BondService.bond(player, ray), "a Cloudray needs Beastbonding 62");
        AbilityTests.level(player, Skill.BEASTBONDING, 62);
        helper.assertFalse(BondService.bond(player, ray), "and an offering of 8 Storm Motes");
        player.getInventory().add(new ItemStack(ModItems.STORM_MOTE.get(), 8));
        helper.assertTrue(BondService.bond(player, ray), "at 62 with 8 Storm Motes it bonds");
        BondCodex.Bond bond = BondService.codex(player).bonds().getFirst();
        Entity out = BondService.summon(player, bond.id);
        double flyBonus = ((LivingEntity) out).getAttribute(Attributes.FLYING_SPEED).getModifier(
                net.minecraft.resources.Identifier.fromNamespaceAndPath(SkillCraft.MODID, "bond_bonus")).amount();
        helper.assertTrue(out instanceof Cloudray && Math.abs(flyBonus - 0.155) < 1e-9, "the summoned Cloudray flies 15.5% faster at 62, got " + flyBonus);

        // The lure refuses low ground (the test floor is far below Y 140)
        ItemStack lure = new ItemStack(ModItems.CLOUDRAY_LURE.get(), 2);
        AbilityTests.hold(player, lure);
        var result = lure.getItem().use(helper.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND);
        helper.assertTrue(result == net.minecraft.world.InteractionResult.FAIL && lure.getCount() == 2, "the lure does nothing below Y 140");
        SkillCraft.LOGGER.info("[bonding] cloudray: needs 62 + 8 Storm Motes; summoned with +{} flying speed; lure refused low", flyBonus);
        helper.succeed();
    }
}
