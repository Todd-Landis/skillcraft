package com.toddlandis.skillcraft.gathering;

import com.toddlandis.skillcraft.SkillCraft;
import com.toddlandis.skillcraft.registry.ModAttachments;
import com.toddlandis.skillcraft.registry.ModDataMaps;
import com.toddlandis.skillcraft.skill.Skill;
import com.toddlandis.skillcraft.skill.SkillGate;
import com.toddlandis.skillcraft.skill.SkillManager;
import com.toddlandis.skillcraft.skill.SkillResource;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

// Level gates and XP for gathering skills (Mining, Woodcutting) on block breaking.
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
        SkillResource resource = resourceOf(event.getState());
        if (resource != null && SkillManager.level(player, resource.skill()) < resource.level()) {
            event.setCanceled(true);
            SkillGate.deny(player, "message.skillcraft.resource_level", resource.skill(), resource.level());
        }
    }

    // Award XP when a natural resource block is actually broken
    @SubscribeEvent
    static void onBreak(BreakBlockEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player) || player.isCreative()) return;
        SkillResource resource = resourceOf(event.getState());
        if (resource == null) return;

        Level level = (Level) event.getLevel();
        LevelChunk chunk = level.getChunkAt(event.getPos());
        if (chunk.getData(ModAttachments.PLACED_BLOCKS).remove(event.getPos())) {
            chunk.markUnsaved();
            return; // player-placed: no XP
        }
        if (!player.hasCorrectToolForDrops(event.getState(), level, event.getPos())) return;

        SkillManager.awardXp(player, resource.skill(), resource.xp());
    }

    // Remember player-placed resource blocks so they can't be farmed for XP
    @SubscribeEvent
    static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof Player) || !(event.getLevel() instanceof Level level) || level.isClientSide()) return;
        if (resourceOf(event.getPlacedBlock()) == null) return;

        LevelChunk chunk = level.getChunkAt(event.getPos());
        chunk.getData(ModAttachments.PLACED_BLOCKS).add(event.getPos());
        chunk.markUnsaved();
    }

    private static SkillResource resourceOf(BlockState state) {
        return state.typeHolder().getData(ModDataMaps.BLOCK_RESOURCES);
    }
}
