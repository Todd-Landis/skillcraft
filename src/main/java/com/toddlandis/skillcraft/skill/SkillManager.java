package com.toddlandis.skillcraft.skill;

import com.toddlandis.skillcraft.Config;
import com.toddlandis.skillcraft.combat.Constitution;
import com.toddlandis.skillcraft.network.XpGainPayload;
import com.toddlandis.skillcraft.registry.ModAttachments;
import com.toddlandis.skillcraft.registry.ModDataMaps;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

// The one place that reads and changes skill XP. Gameplay code should go through here
// rather than touching PlayerSkills directly, so syncing and level-ups always happen.
public final class SkillManager {
    private SkillManager() {}

    public static PlayerSkills get(Player player) {
        return player.getData(ModAttachments.SKILLS);
    }

    public static int level(Player player, Skill skill) {
        return get(player).getLevel(skill);
    }

    // XP from normal gameplay; scaled by the server's xpMultiplier config
    public static void awardXp(ServerPlayer player, Skill skill, double baseXp) {
        addXp(player, skill, baseXp * Config.XP_MULTIPLIER.get());
    }

    // Raw XP change (used by commands); announces level-ups and sends an XP drop
    public static void addXp(ServerPlayer player, Skill skill, double amount) {
        if (amount <= 0) return;
        PlayerSkills skills = get(player);
        int oldLevel = skills.getLevel(skill);
        skills.setXp(skill, skills.getXp(skill) + amount);
        int newLevel = skills.getLevel(skill);
        player.setData(ModAttachments.SKILLS, skills); // triggers sync to the client

        boolean leveledUp = newLevel > oldLevel;
        PacketDistributor.sendToPlayer(player, new XpGainPayload(skill, (float) amount, leveledUp ? newLevel : 0));
        if (leveledUp && skill == Skill.CONSTITUTION) Constitution.apply(player);
        if (leveledUp) com.toddlandis.skillcraft.ability.AbilityService.onLevelUp(player, skill, oldLevel, newLevel);
        if (leveledUp) {
            player.sendSystemMessage(Component.translatable("message.skillcraft.level_up", skill.displayName(), newLevel)
                    .withStyle(ChatFormatting.GOLD));
        }
    }

    public static void setXp(ServerPlayer player, Skill skill, double xp) {
        PlayerSkills skills = get(player);
        skills.setXp(skill, xp);
        player.setData(ModAttachments.SKILLS, skills);
        if (skill == Skill.CONSTITUTION) Constitution.apply(player);
    }

    // Returns the requirements for an item, or null if it has none
    public static SkillRequirements requirements(ItemStack stack) {
        return stack.isEmpty() ? null : stack.typeHolder().getData(ModDataMaps.ITEM_REQUIREMENTS);
    }

    // Returns the level still needed to use this item for this skill, or 0 if the player can use it
    public static int missingLevel(Player player, ItemStack stack, Skill skill) {
        SkillRequirements req = requirements(stack);
        if (req == null || !req.levels().containsKey(skill)) return 0;
        int needed = req.levelFor(skill);
        return level(player, skill) >= needed ? 0 : needed;
    }
}
