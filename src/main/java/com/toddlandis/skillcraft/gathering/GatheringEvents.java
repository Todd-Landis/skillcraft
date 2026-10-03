package com.toddlandis.skillcraft.gathering;

import com.toddlandis.skillcraft.SkillCraft;
import com.toddlandis.skillcraft.registry.ModDataMaps;
import com.toddlandis.skillcraft.skill.Skill;
import com.toddlandis.skillcraft.skill.SkillGate;
import com.toddlandis.skillcraft.skill.SkillManager;
import com.toddlandis.skillcraft.skill.SkillResource;

import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

// Level gates for breaking blocks in the world. World blocks give no XP;
// gathering skills are trained on skilling nodes (see node/GatheringSessions).
@EventBusSubscriber(modid = SkillCraft.MODID)
public class GatheringEvents {
    private static final Skill[] TOOL_SKILLS = {Skill.MINING, Skill.WOODCUTTING};

    // Fires every tick while mining. Cancelling it stops the block from breaking at all.
    @SubscribeEvent
    static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        if (player.isCreative()) return;

        // Is the held tool too high-tier for the player?
        for (Skill skill : TOOL_SKILLS) {
            int needed = SkillManager.missingLevel(player, player.getMainHandItem(), skill);
            if (needed > 0) {
                event.setCanceled(true);
                SkillGate.deny(player, "message.skillcraft.tool_level", skill, needed);
                return;
            }
        }

        // Is the block itself level-gated?
        SkillResource resource = event.getState().typeHolder().getData(ModDataMaps.BLOCK_RESOURCES);
        if (resource != null && SkillManager.level(player, resource.skill()) < resource.level()) {
            event.setCanceled(true);
            SkillGate.deny(player, "message.skillcraft.resource_level", resource.skill(), resource.level());
        }
    }
}
