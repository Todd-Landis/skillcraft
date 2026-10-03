package com.toddlandis.skillcraft.combat;

import com.toddlandis.skillcraft.SkillCraft;
import com.toddlandis.skillcraft.skill.Skill;
import com.toddlandis.skillcraft.skill.SkillGate;
import com.toddlandis.skillcraft.skill.SkillManager;

import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;

// Weapon level requirements. Combat XP comes in Phase 5.
@EventBusSubscriber(modid = SkillCraft.MODID)
public class CombatGateEvents {
    @SubscribeEvent
    static void onAttack(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (player.isCreative()) return;
        int needed = SkillManager.missingLevel(player, player.getMainHandItem(), Skill.MELEE);
        if (needed > 0) {
            event.setCanceled(true);
            SkillGate.deny(player, "message.skillcraft.weapon_level", Skill.MELEE, needed);
        }
    }

    // Drawing a bow or loading a crossbow
    @SubscribeEvent
    static void onStartUsingItem(LivingEntityUseItemEvent.Start event) {
        if (!(event.getEntity() instanceof Player player) || player.isCreative()) return;
        int needed = SkillManager.missingLevel(player, event.getItem(), Skill.RANGED);
        if (needed > 0) {
            event.setCanceled(true);
            SkillGate.deny(player, "message.skillcraft.weapon_level", Skill.RANGED, needed);
        }
    }
}
