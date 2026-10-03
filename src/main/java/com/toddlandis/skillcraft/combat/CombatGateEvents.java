package com.toddlandis.skillcraft.combat;

import com.toddlandis.skillcraft.SkillCraft;
import com.toddlandis.skillcraft.registry.ModItems;
import com.toddlandis.skillcraft.skill.Skill;
import com.toddlandis.skillcraft.skill.SkillGate;
import com.toddlandis.skillcraft.skill.SkillManager;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;

// Equipment level requirements: weapons need Melee/Ranged, ammo needs Ranged, armour needs
// Defence. Also enforces the two-handed Emberfall Maul (no shield while it's held).
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

    // Drawing a bow or loading a crossbow (the weapon and the ammo it would fire), or raising a shield
    @SubscribeEvent
    static void onStartUsingItem(LivingEntityUseItemEvent.Start event) {
        if (!(event.getEntity() instanceof Player player) || player.isCreative()) return;
        ItemStack item = event.getItem();
        int needed = SkillManager.missingLevel(player, item, Skill.RANGED);
        if (needed == 0 && item.getItem() instanceof ProjectileWeaponItem) {
            needed = SkillManager.missingLevel(player, player.getProjectile(item), Skill.RANGED);
        }
        if (needed > 0) {
            event.setCanceled(true);
            SkillGate.deny(player, "message.skillcraft.weapon_level", Skill.RANGED, needed);
            return;
        }
        if (item.has(DataComponents.BLOCKS_ATTACKS) && player.getMainHandItem().is(ModItems.EMBERFALL_MAUL.get())) {
            event.setCanceled(true);
            player.sendOverlayMessage(Component.translatable("message.skillcraft.two_handed").withStyle(ChatFormatting.RED));
        }
    }

    // Armour above your Defence level is taken off again and put back in your inventory
    @SubscribeEvent
    static void onEquip(LivingEquipmentChangeEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.isCreative() || player.isSpectator()) return;
        EquipmentSlot slot = event.getSlot();
        if (slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR) return;
        ItemStack worn = event.getTo();
        int needed = SkillManager.missingLevel(player, worn, Skill.DEFENCE);
        if (needed <= 0) return;
        ItemStack removed = worn.copy();
        player.setItemSlot(slot, ItemStack.EMPTY);
        if (!player.getInventory().add(removed)) player.drop(removed, false);
        SkillGate.deny(player, "message.skillcraft.armor_level", Skill.DEFENCE, needed);
    }
}
