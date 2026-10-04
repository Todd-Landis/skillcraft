package com.toddlandis.skillcraft.arena;

import com.mojang.serialization.MapCodec;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

// Appears in the arena after a win. Each eligible player right-clicks it to collect their own loot.
public class RewardChestBlock extends Block {
    public static final MapCodec<RewardChestBlock> CODEC = simpleCodec(RewardChestBlock::new);
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 12, 15);

    public RewardChestBlock(Properties properties) {
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
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.SUCCESS;
        ArenaManager manager = ArenaManager.get(serverPlayer.level().getServer());
        ArenaInstance instance = manager.instanceOf(serverPlayer);
        if (instance == null || instance.state() != ArenaInstance.State.VICTORY) return InteractionResult.SUCCESS;
        if (!instance.hasLoot(serverPlayer.getUUID())) {
            serverPlayer.sendOverlayMessage(Component.translatable("message.skillcraft.reward_chest_empty").withStyle(ChatFormatting.GRAY));
            return InteractionResult.SUCCESS;
        }
        manager.giveLoot(serverPlayer, instance);
        level.playSound(null, pos, SoundEvents.VAULT_OPEN_SHUTTER, SoundSource.BLOCKS, 1f, 1f);
        return InteractionResult.SUCCESS;
    }
}
