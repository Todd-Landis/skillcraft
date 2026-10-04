package com.toddlandis.skillcraft.bonding;

import com.mojang.serialization.MapCodec;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

// The Rekindling Shrine (docs/BEASTBONDING.md section 5): where Fallen bonds are brought back.
// Stand within a few blocks, open the Bond Codex and press Rekindle; BondService checks for a
// shrine nearby and takes the tribute. Using the shrine itself explains that.
public class RekindlingShrineBlock extends Block {
    public static final MapCodec<RekindlingShrineBlock> CODEC = simpleCodec(RekindlingShrineBlock::new);
    private static final VoxelShape SHAPE = Shapes.or(Block.box(1, 0, 1, 15, 4, 15), Block.box(4, 4, 4, 12, 9, 12), Block.box(3, 9, 3, 13, 12, 13));

    public RekindlingShrineBlock(Properties properties) {
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
        if (!level.isClientSide()) {
            player.sendOverlayMessage(Component.translatable("message.skillcraft.shrine_hint").withStyle(ChatFormatting.GOLD));
        }
        return InteractionResult.SUCCESS;
    }

    // Soul flames drift up from the bowl
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) != 0) return;
        level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, pos.getX() + 0.35 + random.nextDouble() * 0.3, pos.getY() + 0.8, pos.getZ() + 0.35 + random.nextDouble() * 0.3, 0, 0.02, 0);
    }
}
