package com.toddlandis.skillcraft.fishing;

import com.toddlandis.skillcraft.altar.AttunementAltarBlock;
import com.toddlandis.skillcraft.node.NodeType;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

// A shoal of fish, made at the Attunement Altar. Use it on open water to release it as a fishing
// spot. The water rules are in FishingWater; spacing works like nodes (at most spacingMax() spots
// of the same kind within spacingRadius() blocks).
public class ShoalItem extends Item {
    private final NodeType type;

    public ShoalItem(NodeType type, Properties properties) {
        super(properties);
        this.type = type;
    }

    public NodeType type() {
        return type;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() != HitResult.Type.BLOCK) return InteractionResult.PASS;
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) return InteractionResult.SUCCESS;

        BlockPos water = hit.getBlockPos();
        String problem = FishingWater.problem(level, water, type.minWaterArea());
        if (problem != null) {
            serverPlayer.sendOverlayMessage(Component.translatable(problem, type.minWaterArea()).withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }
        AABB area = new AABB(water).inflate(type.spacingRadius());
        int nearby = level.getEntitiesOfClass(FishingSpotEntity.class, area, spot -> spot.type() == type).size();
        if (nearby >= type.spacingMax()) {
            serverPlayer.sendOverlayMessage(Component.translatable("message.skillcraft.node_too_close",
                    AttunementAltarBlock.nodeName(type), type.spacingMax(), type.spacingRadius()).withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }
        if (!level.getEntitiesOfClass(FishingSpotEntity.class, new AABB(water)).isEmpty()) return InteractionResult.FAIL;

        FishingSpotEntity spot = FishingSpotEntity.create(serverLevel, type, water);
        serverLevel.addFreshEntity(spot);
        serverLevel.playSound(null, water, SoundEvents.FISH_SWIM, SoundSource.NEUTRAL, 1.0f, 1.0f);
        serverLevel.sendParticles(ParticleTypes.SPLASH, spot.getX(), water.getY() + FishingSpotEntity.SURFACE_Y, spot.getZ(), 25, 0.4, 0.1, 0.4, 0.2);
        if (!player.getAbilities().instabuild) player.getItemInHand(hand).shrink(1);
        return InteractionResult.SUCCESS;
    }
}
