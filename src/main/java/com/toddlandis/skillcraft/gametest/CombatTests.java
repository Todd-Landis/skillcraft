package com.toddlandis.skillcraft.gametest;

import java.util.UUID;

import com.mojang.authlib.GameProfile;
import com.toddlandis.skillcraft.combat.CombatDamage;
import com.toddlandis.skillcraft.combat.Constitution;
import com.toddlandis.skillcraft.combat.DamageStyle;
import com.toddlandis.skillcraft.combat.DamageStyles;
import com.toddlandis.skillcraft.registry.ModItems;
import com.toddlandis.skillcraft.skill.Skill;
import com.toddlandis.skillcraft.skill.SkillManager;
import com.toddlandis.skillcraft.skill.XpTable;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

// Combat GameTests: data maps load and drive matchups and typed armour; damage to mobs pays
// combat XP (and babies don't); Constitution raises max health.
public final class CombatTests {
    private CombatTests() {}

    static void matchups(GameTestHelper helper) {
        Skeleton skeleton = helper.spawnWithNoFreeWill(EntityTypes.SKELETON, new Vec3(4, 1, 4));
        helper.assertValueEqual(DamageStyles.modifier(skeleton, DamageStyle.CRUSH), 1.4, "skeleton crush weakness");
        helper.assertValueEqual(DamageStyles.modifier(skeleton, DamageStyle.STAB), 0.7, "skeleton stab resistance");
        helper.assertTrue(DamageStyles.weapon(new ItemStack(ModItems.WARHAMMERS.get("iron").get())) == DamageStyle.CRUSH, "warhammers crush");
        helper.assertTrue(DamageStyles.weapon(new ItemStack(net.minecraft.world.item.Items.IRON_SWORD)) == DamageStyle.SLASH, "swords slash");
        helper.assertTrue(DamageStyles.weapon(new ItemStack(net.minecraft.world.item.Items.IRON_SPEAR)) == DamageStyle.STAB, "spears stab");

        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.PADDED_HELMET.get()));
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.PADDED_CHESTPLATE.get()));
        player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModItems.PADDED_LEGGINGS.get()));
        player.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.PADDED_BOOTS.get()));
        double crush = CombatDamage.typedDefence(player, DamageStyle.Group.CRUSH);
        helper.assertTrue(Math.abs(crush - 18) < 0.01, "full padded set should give +18% crush defence, got " + crush);
        helper.succeed();
    }

    static void combatXp(GameTestHelper helper) {
        // The vanilla mock player is always in creative (which earns no combat XP), so use a fake survival player
        ServerPlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "skillcraft_xp_test"));
        player.setGameMode(GameType.SURVIVAL);
        Zombie zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new Vec3(4, 1, 4));
        float before = zombie.getHealth();
        boolean hurt = zombie.hurtServer(helper.getLevel(), helper.getLevel().damageSources().playerAttack(player), 6f);
        double melee = SkillManager.get(player).getXp(Skill.MELEE);
        double constitution = SkillManager.get(player).getXp(Skill.CONSTITUTION);
        helper.assertTrue(melee > 0, "no Melee XP for hitting a zombie (hurt=" + hurt + ", health " + before + " -> " + zombie.getHealth()
                + ", mode=" + player.gameMode() + ", baby=" + zombie.isBaby() + ", constitution=" + constitution + ")");
        helper.assertTrue(Math.abs(melee / constitution - 3.0) < 0.01, "Melee:Constitution XP should be 4:1.33, was " + melee + ":" + constitution);
        helper.assertTrue(melee <= 6 * 4.0 * 1.06, "XP should follow damage dealt (got " + melee + ")");

        Zombie baby = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new Vec3(10, 1, 10));
        baby.setBaby(true);
        baby.hurtServer(helper.getLevel(), helper.getLevel().damageSources().playerAttack(player), 6f);
        helper.assertValueEqual(SkillManager.get(player).getXp(Skill.MELEE), melee, "baby mobs must not give XP");
        helper.succeed();
    }

    static void constitution(GameTestHelper helper) {
        helper.assertValueEqual(Constitution.bonusHealth(1), 0, "level 1 bonus");
        helper.assertValueEqual(Constitution.bonusHealth(35), 6, "level 35 bonus");
        helper.assertValueEqual(Constitution.bonusHealth(99), 20, "level 99 bonus");
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        SkillManager.setXp(player, Skill.CONSTITUTION, XpTable.xpForLevel(40));
        helper.assertValueEqual(player.getMaxHealth(), 28f, "max health at Constitution 40");
        helper.succeed();
    }
}
