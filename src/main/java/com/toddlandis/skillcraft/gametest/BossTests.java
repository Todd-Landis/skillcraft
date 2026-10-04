package com.toddlandis.skillcraft.gametest;

import com.toddlandis.skillcraft.SkillCraft;
import com.toddlandis.skillcraft.boss.SlagheartAction;
import com.toddlandis.skillcraft.boss.SlagheartEntity;
import com.toddlandis.skillcraft.combat.DamageStyle;
import com.toddlandis.skillcraft.combat.DamageStyles;
import com.toddlandis.skillcraft.registry.ModEntities;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.monster.cubemob.MagmaCube;
import net.minecraft.world.phys.Vec3;

// Boss GameTests: Slagheart spawns, wakes up, changes phase at its health thresholds (spawning
// its Cinder Mites and taking reduced damage while they live), and reads its stance tables.
public final class BossTests {
    private BossTests() {}

    private static SlagheartEntity spawn(GameTestHelper helper) {
        SlagheartEntity boss = helper.spawn(ModEntities.SLAGHEART.get(), new Vec3(8, 1, 8));
        boss.setup(-1, helper.absoluteVec(new Vec3(8, 1, 8)), 1);
        return boss;
    }

    static void phases(GameTestHelper helper) {
        SlagheartEntity boss = spawn(helper);
        helper.assertValueEqual(boss.getMaxHealth(), 350f, "boss health from boss_tuning");
        helper.assertTrue(boss.action() == SlagheartAction.AWAKEN, "boss should start by awakening");
        helper.assertFalse(boss.hurtServer(helper.getLevel(), helper.getLevel().damageSources().generic(), 5f), "boss took damage while awakening");

        helper.runAfterDelay(70, () -> {
            helper.assertTrue(boss.phase() == 1, "boss should be in phase 1");
            boss.setHealth(boss.getMaxHealth() * 0.70f);
            boss.hurtServer(helper.getLevel(), helper.getLevel().damageSources().generic(), 30f);
            helper.assertTrue(boss.phase() == 2, "phase 2 did not start below 65% health (health " + boss.getHealth() + ")");
            helper.assertTrue(boss.action() == SlagheartAction.ROAR, "phase change should roar");
        });
        helper.runAfterDelay(130, () -> {
            helper.assertTrue(!helper.getLevel().getEntitiesOfClass(MagmaCube.class, boss.getBoundingBox().inflate(8),
                    mite -> mite.entityTags().contains(SlagheartEntity.MITE_TAG)).isEmpty(), "Cinder Mites did not spawn in phase 2");
            helper.assertTrue(boss.isStoked(), "boss should be stoked while mites live");
            float before = boss.getHealth();
            boss.hurtServer(helper.getLevel(), helper.getLevel().damageSources().generic(), 10f);
            float taken = before - boss.getHealth();
            helper.assertTrue(taken > 0 && taken < 6f, "stoked boss should take reduced damage, took " + taken);
            SkillCraft.LOGGER.info("[boss_phases] phase 2 with mites: 10 raw damage dealt {} (stoked + armour)", taken);
        });
        helper.runAfterDelay(160, () -> {
            boss.setHealth(boss.getMaxHealth() * 0.32f);
            boss.hurtServer(helper.getLevel(), helper.getLevel().damageSources().generic(), 30f);
            helper.assertTrue(boss.phase() == 3, "phase 3 did not start below 30% health");
            SkillCraft.LOGGER.info("[boss_phases] phase 3 reached at {} / {} health, tick {}", boss.getHealth(), boss.getMaxHealth(), helper.getTick());
        });
        helper.runAfterDelay(230, () -> {
            boss.discard();
            helper.succeed();
        });
    }

    static void stances(GameTestHelper helper) {
        SlagheartEntity boss = spawn(helper);
        helper.assertValueEqual(DamageStyles.modifier(boss, DamageStyle.CRUSH), 1.25, "banked crush modifier");
        helper.assertValueEqual(DamageStyles.modifier(boss, DamageStyle.SLASH), 0.6, "banked slash modifier");
        helper.assertValueEqual(boss.tuning().stanceModifier("vented", DamageStyle.STAB), 1.5, "vented stab modifier");
        boss.discard();
        helper.succeed();
    }
}
