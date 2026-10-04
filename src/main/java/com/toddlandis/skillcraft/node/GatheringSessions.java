package com.toddlandis.skillcraft.node;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.toddlandis.skillcraft.SkillCraft;
import com.toddlandis.skillcraft.registry.ModAttachments;
import com.toddlandis.skillcraft.registry.ModDataMaps;
import com.toddlandis.skillcraft.registry.ModSounds;
import com.toddlandis.skillcraft.skill.Skill;
import com.toddlandis.skillcraft.skill.SkillManager;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

// OSRS-style auto-gathering: right-click a node once and the player keeps swinging at it
// while they stay close and hold a suitable tool. Every SWINGS_PER_ATTEMPT-th swing is a real
// attempt (success roll); the swings in between are just for feel.
@EventBusSubscriber(modid = SkillCraft.MODID)
public final class GatheringSessions {
    // Swing interval: 15 ticks (0.75s) with a tier-0 tool, 1 tick faster per tool tier
    // (stone/copper/gold 14, iron 13, diamond 12, netherite 11 = 0.55s)
    private static final int BASE_SWING_TICKS = 15;
    private static final int MIN_SWING_TICKS = 8;
    private static final int SWINGS_PER_ATTEMPT = 2; // every 2nd swing is an attempt
    private static final double MAX_DISTANCE_SQ = 4.5 * 4.5;

    private record Session(BlockPos pos, NodeBlock node, long nextSwing, int swings) {}

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
        if (isFatigued(player, type)) {
            sendFatigued(player, type);
            return;
        }
        SESSIONS.put(player.getUUID(), new Session(pos, node, player.level().getGameTime() + swingTicks(player), 1));
        player.sendOverlayMessage(Component.translatable(type.skill() == Skill.WOODCUTTING
                ? "message.skillcraft.gather_start_wood" : "message.skillcraft.gather_start_ore"));
        swing(player, player.level(), pos, state, type);
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
        if (level.getGameTime() < session.nextSwing()) return;
        if (isFatigued(player, type)) {
            SESSIONS.remove(player.getUUID());
            sendFatigued(player, type);
            return;
        }

        int swings = session.swings() + 1;
        SESSIONS.put(player.getUUID(), new Session(session.pos(), session.node(), level.getGameTime() + swingTicks(player), swings));
        swing(player, level, session.pos(), state, type);
        if (swings % SWINGS_PER_ATTEMPT == 0) attempt(player, level, session.pos(), state, session.node());
    }

    // Cosmetic swing: arm animation, node hit sound and a few chips
    private static void swing(ServerPlayer player, ServerLevel level, BlockPos pos, BlockState state, NodeType type) {
        player.swing(InteractionHand.MAIN_HAND, true);
        SoundEvent sound = (type.skill() == Skill.WOODCUTTING ? ModSounds.NODE_CHOP : ModSounds.NODE_PICK).get();
        level.playSound(null, pos, sound, SoundSource.PLAYERS, 1.0f, 0.85f + level.getRandom().nextFloat() * 0.3f);
        chips(level, pos, state, 3, 0.05);
    }

    private static void attempt(ServerPlayer player, ServerLevel level, BlockPos pos, BlockState state, NodeBlock node) {
        NodeType type = node.type();
        boolean wood = type.skill() == Skill.WOODCUTTING;
        double chance = type.successChance(SkillManager.level(player, type.skill()), ModDataMaps.gatheringTool(player.getMainHandItem()).power());
        if (level.getRandom().nextDouble() >= chance) return; // missed this attempt

        ItemStack drop = new ItemStack(type.drop(), type.dropCount());
        if (!player.getInventory().add(drop)) player.drop(drop, false);
        SkillManager.awardXp(player, type.skill(), type.xp());
        addFatigue(player, type);
        level.playSound(null, pos, (wood ? ModSounds.NODE_SUCCESS_WOOD : ModSounds.NODE_SUCCESS_ORE).get(), SoundSource.BLOCKS, 0.9f, 0.9f + level.getRandom().nextFloat() * 0.2f);
        chips(level, pos, state, 10, 0.1);

        if (level.getRandom().nextDouble() < type.depleteChance(SkillManager.level(player, type.skill()))) {
            level.playSound(null, pos, (wood ? ModSounds.NODE_DEPLETE_WOOD : ModSounds.NODE_DEPLETE_ORE).get(), SoundSource.BLOCKS, 1.0f, 1.0f);
            chips(level, pos, state, 30, 0.2);
            node.deplete(level, pos, state);
            stop(player, wood ? "message.skillcraft.depleted_wood" : "message.skillcraft.depleted_ore");
        }
    }

    private static void chips(ServerLevel level, BlockPos pos, BlockState state, int count, double speed) {
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), pos.getX() + 0.5, pos.getY() + 0.75, pos.getZ() + 0.5, count, 0.25, 0.2, 0.25, speed);
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

    // Better tools swing faster (tool tier) and succeed more often (tool power).
    // Both come from data/skillcraft/data_maps/item/gathering_tools.json
    public static int swingTicks(ServerPlayer player) {
        return Math.max(MIN_SWING_TICKS, BASE_SWING_TICKS - ModDataMaps.gatheringTool(player.getMainHandItem()).tier());
    }

    // --- Gathering fatigue: a per-player cap on how fast each resource can be gathered ---

    private static final double FATIGUE_WARNING = 0.75;

    public static boolean isFatigued(ServerPlayer player, NodeType type) {
        int level = SkillManager.level(player, type.skill());
        return player.getData(ModAttachments.FATIGUE).isExhausted(type, level, player.level().getGameTime());
    }

    public static void addFatigue(ServerPlayer player, NodeType type) {
        GatheringFatigue fatigue = player.getData(ModAttachments.FATIGUE);
        int level = SkillManager.level(player, type.skill());
        long now = player.level().getGameTime();
        double capacity = type.fatigueCapacity(level);
        boolean wasBelowWarning = fatigue.current(type, level, now) < capacity * FATIGUE_WARNING;
        fatigue.add(type, level, now, 1);
        if (wasBelowWarning && fatigue.current(type, level, now) >= capacity * FATIGUE_WARNING) {
            player.sendSystemMessage(Component.translatable("message.skillcraft.fatigue_warning", type.drop().getDefaultInstance().getHoverName())
                    .withStyle(ChatFormatting.YELLOW));
        }
    }

    public static void sendFatigued(ServerPlayer player, NodeType type) {
        int level = SkillManager.level(player, type.skill());
        int seconds = player.getData(ModAttachments.FATIGUE).secondsUntilReady(type, level, player.level().getGameTime());
        String wait = seconds < 60 ? seconds + "s" : (seconds / 60) + "m " + (seconds % 60) + "s";
        player.sendOverlayMessage(Component.translatable("message.skillcraft.fatigued", type.drop().getDefaultInstance().getHoverName(), wait)
                .withStyle(ChatFormatting.RED));
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
