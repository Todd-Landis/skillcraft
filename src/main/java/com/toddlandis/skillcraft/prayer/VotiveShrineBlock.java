package com.toddlandis.skillcraft.prayer;

import com.mojang.serialization.MapCodec;

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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

// The Votive Shrine (docs/AGILITY_MAGIC_PRAYER.md section 3):
//   - right-click holding offerings (bones, skulls): offer the whole stack for extra Prayer XP
//     (sneaking skips a block's use in vanilla, so sneak + use buries one instead; see PrayerEvents)
//   - right-click with an empty hand: refill your Faith
public class VotiveShrineBlock extends Block {
    public static final MapCodec<VotiveShrineBlock> CODEC = simpleCodec(VotiveShrineBlock::new);
    private static final VoxelShape SHAPE = Shapes.or(Block.box(1, 0, 1, 15, 4, 15), Block.box(4, 4, 4, 12, 12, 12), Block.box(2, 12, 2, 14, 15, 14));

    public VotiveShrineBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (PrayerService.offering(stack) == null) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (player instanceof ServerPlayer serverPlayer) {
            int used = PrayerService.offer(serverPlayer, stack, stack.getCount(), true);
            if (used > 0 && level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 6 + Math.min(used, 20), 0.2, 0.2, 0.2, 0.02);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer serverPlayer) {
            boolean wasFull = PrayerService.points(serverPlayer) >= PrayerService.maxFaith(serverPlayer);
            PrayerService.restore(serverPlayer);
            level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.6f, wasFull ? 1.6f : 1.2f);
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 12, 0.3, 0.4, 0.3, 0.03);
            }
            serverPlayer.sendOverlayMessage(Component.translatable("message.skillcraft.faith_restored", Math.round(PrayerService.maxFaith(serverPlayer)))
                    .withStyle(ChatFormatting.GOLD));
        }
        return InteractionResult.SUCCESS;
    }
}
