package com.toddlandis.skillcraft.combat;

import com.toddlandis.skillcraft.SkillCraft;
import com.toddlandis.skillcraft.skill.Skill;
import com.toddlandis.skillcraft.skill.SkillManager;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

// Constitution raises max health: +1 heart per 10 levels, and one more at 99 (20 -> 40 HP).
// Low levels feel like vanilla; a maxed player has double health. See docs/COMBAT.md.
@EventBusSubscriber(modid = SkillCraft.MODID)
public final class Constitution {
    public static final Identifier MODIFIER_ID = Identifier.fromNamespaceAndPath(SkillCraft.MODID, "constitution");

    private Constitution() {}

    public static int bonusHealth(int level) {
        return 2 * (level / 10) + (level >= 99 ? 2 : 0);
    }

    // Re-applies the health modifier for the player's current Constitution level
    public static void apply(ServerPlayer player) {
        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth == null) return;
        int bonus = bonusHealth(SkillManager.level(player, Skill.CONSTITUTION));
        AttributeModifier current = maxHealth.getModifier(MODIFIER_ID);
        if (current != null && current.amount() == bonus) return;
        maxHealth.removeModifier(MODIFIER_ID);
        if (bonus > 0) maxHealth.addPermanentModifier(new AttributeModifier(MODIFIER_ID, bonus, AttributeModifier.Operation.ADD_VALUE));
        if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
    }

    @SubscribeEvent
    static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) apply(player);
    }

    @SubscribeEvent
    static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            apply(player);
            if (!event.isEndConquered()) player.setHealth(player.getMaxHealth());
        }
    }
}
