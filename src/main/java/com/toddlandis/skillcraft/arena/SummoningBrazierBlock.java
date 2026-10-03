package com.toddlandis.skillcraft.arena;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.MapCodec;
import com.toddlandis.skillcraft.registry.ModItems;

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
import net.minecraft.world.phys.shapes.VoxelShape;

// The overworld entrance to the boss arena. Right-click it with an Ember Sigil: the sigil is
// used up and you, plus up to 3 players standing within 5 blocks, enter a fresh Slagheart instance.
public class SummoningBrazierBlock extends Block {
    public static final MapCodec<SummoningBrazierBlock> CODEC = simpleCodec(SummoningBrazierBlock::new);
    public static final double PARTY_RADIUS = 5.0;
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 14, 14);

    public SummoningBrazierBlock(Properties properties) {
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
        if (!stack.is(ModItems.EMBER_SIGIL.get())) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!(player instanceof ServerPlayer owner) || !(level instanceof ServerLevel serverLevel)) return InteractionResult.SUCCESS;
        if (ArenaDimension.is(level)) return InteractionResult.FAIL;
        ArenaManager manager = ArenaManager.get(serverLevel.getServer());
        if (manager.isInInstance(owner)) return InteractionResult.FAIL;

        List<ServerPlayer> party = new ArrayList<>();
        party.add(owner);
        for (ServerPlayer other : serverLevel.players()) {
            if (party.size() >= ArenaManager.MAX_PARTY) break;
            if (other == owner || other.isSpectator() || !other.isAlive() || manager.isInInstance(other)) continue;
            if (other.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= PARTY_RADIUS * PARTY_RADIUS) party.add(other);
        }
        level.playSound(null, pos, SoundEvents.BLAZE_SHOOT, SoundSource.BLOCKS, 1f, 0.6f);
        serverLevel.sendParticles(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 40, 0.3, 0.5, 0.3, 0.05);
        if (manager.start(owner, party).isEmpty()) {
            owner.sendOverlayMessage(Component.translatable("message.skillcraft.arena_unavailable").withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }
        if (!player.isCreative()) stack.shrink(1);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendOverlayMessage(Component.translatable("message.skillcraft.brazier_hint").withStyle(ChatFormatting.GOLD));
        }
        return InteractionResult.SUCCESS;
    }
}
