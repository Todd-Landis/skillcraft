package com.toddlandis.skillcraft.node;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.toddlandis.skillcraft.SkillCraft;
import com.toddlandis.skillcraft.skill.Skill;
import com.toddlandis.skillcraft.skill.SkillManager;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

// OSRS-style auto-gathering: right-click a node once and the player keeps making attempts
// every ATTEMPT_TICKS while they stay close and hold a suitable tool.
@EventBusSubscriber(modid = SkillCraft.MODID)
public final class GatheringSessions {
    private static final int ATTEMPT_TICKS = 30; // 1.5 seconds
    private static final double MAX_DISTANCE_SQ = 4.5 * 4.5;

    private record Session(BlockPos pos, NodeBlock node, long nextAttempt) {}

    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    private GatheringSessions() {}

    public static void start(ServerPlayer player, BlockPos pos, NodeBlock node, BlockState state) {
        NodeType type = node.type();
        Session current = SESSIONS.get(player.getUUID());
        if (current != null && current.pos().equals(pos)) {
            stop(player, "message.skillcraft.gather_stop"); // right-clicking again stops
            return;
        }
        if (state.getValue(NodeBlock.DEPLETED)) {
            player.sendOverlayMessage(Component.translatable("message.skillcraft.node_depleted").withStyle(ChatFormatting.GRAY));
            return;
        }
        String problem = checkCanGather(player, type);
        if (problem != null) {
            player.sendOverlayMessage(Component.translatable(problem, type.skill().displayName(), type.level()).withStyle(ChatFormatting.RED));
            return;
        }
        SESSIONS.put(player.getUUID(), new Session(pos, node, player.level().getGameTime() + ATTEMPT_TICKS));
        player.sendOverlayMessage(Component.translatable(type.skill() == Skill.WOODCUTTING
                ? "message.skillcraft.gather_start_wood" : "message.skillcraft.gather_start_ore"));
        player.swing(InteractionHand.MAIN_HAND, true);
    }

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Session session = SESSIONS.get(player.getUUID());
        if (session == null) return;

        ServerLevel level = player.level();
        BlockState state = level.getBlockState(session.pos());
        if (state.getBlock() != session.node() || state.getValue(NodeBlock.DEPLETED)) {
            SESSIONS.remove(player.getUUID());
            return;
        }
        if (player.distanceToSqr(Vec3.atCenterOf(session.pos())) > MAX_DISTANCE_SQ) {
            stop(player, "message.skillcraft.gather_too_far");
            return;
        }
        NodeType type = session.node().type();
        if (checkCanGather(player, type) != null) {
            stop(player, "message.skillcraft.gather_stop");
            return;
        }
        if (level.getGameTime() < session.nextAttempt()) return;

        SESSIONS.put(player.getUUID(), new Session(session.pos(), session.node(), level.getGameTime() + ATTEMPT_TICKS));
        attempt(player, level, session.pos(), state, session.node());
    }

    private static void attempt(ServerPlayer player, ServerLevel level, BlockPos pos, BlockState state, NodeBlock node) {
        NodeType type = node.type();
        player.swing(InteractionHand.MAIN_HAND, true);
        level.playSound(null, pos, state.getSoundType(level, pos, player).getHitSound(), SoundSource.BLOCKS, 0.8f, 0.9f + level.getRandom().nextFloat() * 0.2f);

        double chance = type.successChance(SkillManager.level(player, type.skill()), toolPower(player.getMainHandItem(), type));
        if (level.getRandom().nextDouble() >= chance) return; // missed this attempt

        ItemStack drop = new ItemStack(type.drop(), type.dropCount());
        if (!player.getInventory().add(drop)) player.drop(drop, false);
        SkillManager.awardXp(player, type.skill(), type.xp());
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 8, 0.25, 0.2, 0.25, 0.05);

        if (level.getRandom().nextDouble() < type.depleteChance()) {
            node.deplete(level, pos, state);
            stop(player, type.skill() == Skill.WOODCUTTING ? "message.skillcraft.depleted_wood" : "message.skillcraft.depleted_ore");
        }
    }

    // Returns a translation key describing why the player can't gather, or null if they can
    private static String checkCanGather(ServerPlayer player, NodeType type) {
        if (SkillManager.level(player, type.skill()) < type.level()) return "message.skillcraft.resource_level";
        ItemStack tool = player.getMainHandItem();
        boolean rightTool = type.skill() == Skill.WOODCUTTING ? tool.is(ItemTags.AXES) : tool.is(ItemTags.PICKAXES);
        if (!rightTool) return type.skill() == Skill.WOODCUTTING ? "message.skillcraft.need_axe" : "message.skillcraft.need_pickaxe";
        if (SkillManager.missingLevel(player, tool, type.skill()) > 0) return "message.skillcraft.tool_too_advanced";
        return null;
    }

    // Better tools succeed more often. Based on the tool's vanilla mining speed:
    // wood 0.85, stone 1.0, iron 1.15, diamond 1.3, netherite 1.375, gold 1.6
    private static double toolPower(ItemStack tool, NodeType type) {
        BlockState reference = type.skill() == Skill.WOODCUTTING ? Blocks.OAK_LOG.defaultBlockState() : Blocks.STONE.defaultBlockState();
        return 0.7 + 0.075 * tool.getDestroySpeed(reference);
    }

    private static void stop(ServerPlayer player, String messageKey) {
        SESSIONS.remove(player.getUUID());
        player.sendOverlayMessage(Component.translatable(messageKey).withStyle(ChatFormatting.GRAY));
    }

    @SubscribeEvent
    static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SESSIONS.remove(event.getEntity().getUUID());
    }
}
