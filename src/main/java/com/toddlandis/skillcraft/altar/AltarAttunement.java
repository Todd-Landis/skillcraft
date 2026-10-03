package com.toddlandis.skillcraft.altar;

import com.toddlandis.skillcraft.network.AttunementResultPayload;
import com.toddlandis.skillcraft.node.NodeType;
import com.toddlandis.skillcraft.registry.ModBlocks;
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
import net.neoforged.neoforge.network.handling.IPayloadContext;

// Server side of the attunement minigame result.
public final class AltarAttunement {
    // XP for a successful attunement, as a multiple of one node success
    private static final double XP_MULTIPLE = 5;
    // Share of the loaded material lost on a failed attunement
    private static final double FAIL_LOSS = 0.25;
    private static final double MAX_DISTANCE_SQ = 8 * 8;

    private AltarAttunement() {}

    public static void handleResult(AttunementResultPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        ServerLevel level = player.level();
        BlockPos pos = payload.pos();
        if (player.distanceToSqr(Vec3.atCenterOf(pos)) > MAX_DISTANCE_SQ) return;
        if (!(level.getBlockEntity(pos) instanceof AttunementAltarBlockEntity altar) || !altar.isFull()) return;

        NodeType type = altar.target();
        if (payload.success()) {
            altar.clear();
            ItemEntity node = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5,
                    new ItemStack(ModBlocks.NODES.get(type).get()));
            node.setDeltaMovement(0, 0.2, 0);
            level.addFreshEntity(node);
            level.sendParticles(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 40, 0.4, 0.4, 0.4, 0.5);
            level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.8f, 1.4f);
            SkillManager.awardXp(player, type.skill(), type.xp() * XP_MULTIPLE);
            player.sendOverlayMessage(Component.translatable("message.skillcraft.attune_success", AttunementAltarBlock.nodeName(type)).withStyle(ChatFormatting.GREEN));
        } else {
            altar.loseFraction(FAIL_LOSS);
            level.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 20, 0.3, 0.2, 0.3, 0.02);
            level.playSound(null, pos, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.5f, 1.5f);
            player.sendOverlayMessage(Component.translatable("message.skillcraft.attune_fail").withStyle(ChatFormatting.RED));
        }
    }
}
