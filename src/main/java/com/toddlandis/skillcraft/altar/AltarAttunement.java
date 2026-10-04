package com.toddlandis.skillcraft.altar;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.toddlandis.skillcraft.SkillCraft;
import com.toddlandis.skillcraft.altar.minigame.MinigameLogic;
import com.toddlandis.skillcraft.altar.minigame.Minigames;
import com.toddlandis.skillcraft.network.AttunementResultPayload;
import com.toddlandis.skillcraft.network.OpenAttunementPayload;
import com.toddlandis.skillcraft.node.NodeType;
import com.toddlandis.skillcraft.registry.ModItems;
import com.toddlandis.skillcraft.skill.SkillManager;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

// Server side of the attunement minigame. The server picks a random seed and remembers it;
// the client plays and sends back its inputs; the server replays the same minigame with the
// same seed and inputs to decide success. A client can't just claim it won.
@EventBusSubscriber(modid = SkillCraft.MODID)
public final class AltarAttunement {
    // XP for a successful attunement, as a multiple of one node success
    private static final double XP_MULTIPLE = 5;
    // Share of the loaded material lost on a failed attunement
    private static final double FAIL_LOSS = 0.25;
    private static final double MAX_DISTANCE_SQ = 8 * 8;
    // Allowed difference (in ticks) between the game length the client claims and real time passed.
    // A client can't have played longer than the time that actually went by.
    private static final int TIMING_SLACK_TICKS = 40;

    private record Session(BlockPos pos, NodeType type, long seed, long startMillis) {}

    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    private AltarAttunement() {}

    public static void open(ServerPlayer player, BlockPos pos, NodeType type) {
        long seed = player.level().getRandom().nextLong();
        SESSIONS.put(player.getUUID(), new Session(pos.immutable(), type, seed, System.currentTimeMillis()));
        PacketDistributor.sendToPlayer(player, new OpenAttunementPayload(pos, type, seed));
    }

    public static void handleResult(AttunementResultPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        Session session = SESSIONS.remove(player.getUUID());
        if (session == null || !session.pos().equals(payload.pos())) return;

        ServerLevel level = player.level();
        BlockPos pos = session.pos();
        if (player.distanceToSqr(Vec3.atCenterOf(pos)) > MAX_DISTANCE_SQ) return;
        if (!(level.getBlockEntity(pos) instanceof AttunementAltarBlockEntity altar)) return;
        if (!altar.isFull() || altar.target() != session.type() || !altar.isOwner(player)) return;

        NodeType type = session.type();
        Minigames.ReplayResult result = Minigames.replay(type.minigame(), type.tierIndex(), session.seed(), payload.inputs());
        // Real time, not server ticks, so server lag can't make an honest game look too long
        long elapsed = (System.currentTimeMillis() - session.startMillis()) / 50;
        boolean timingOk = result.ticks() <= elapsed + TIMING_SLACK_TICKS;

        if (result.state() == MinigameLogic.State.SUCCESS && timingOk) {
            succeed(player, level, pos, altar, type);
        } else if (result.state() == MinigameLogic.State.RUNNING && !result.started()) {
            // Closed without playing: no penalty
        } else {
            if (!timingOk) SkillCraft.LOGGER.warn("Rejected attunement from {}: claimed {} ticks but only {} passed", player.getName().getString(), result.ticks(), elapsed);
            fail(player, level, pos, altar);
        }
    }

    private static void succeed(ServerPlayer player, ServerLevel level, BlockPos pos, AttunementAltarBlockEntity altar, NodeType type) {
        altar.clear();
        ItemEntity node = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5,
                new ItemStack(ModItems.NODES.get(type).get()));
        node.setDeltaMovement(0, 0.2, 0);
        level.addFreshEntity(node);
        level.sendParticles(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 40, 0.4, 0.4, 0.4, 0.5);
        level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.8f, 1.4f);
        SkillManager.awardXp(player, type.skill(), type.xp() * XP_MULTIPLE);
        player.sendOverlayMessage(Component.translatable("message.skillcraft.attune_success", AttunementAltarBlock.nodeName(type)).withStyle(ChatFormatting.GREEN));
    }

    private static void fail(ServerPlayer player, ServerLevel level, BlockPos pos, AttunementAltarBlockEntity altar) {
        altar.loseFraction(FAIL_LOSS);
        level.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 20, 0.3, 0.2, 0.3, 0.02);
        level.playSound(null, pos, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.5f, 1.5f);
        player.sendOverlayMessage(Component.translatable("message.skillcraft.attune_fail").withStyle(ChatFormatting.RED));
    }

    @SubscribeEvent
    static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SESSIONS.remove(event.getEntity().getUUID());
    }
}
