package com.toddlandis.skillcraft.leycraft;

import com.mojang.serialization.MapCodec;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

// The Ley Altar (docs/LEYCRAFT.md): weaves ley shards (or amethyst shards) into motes of whichever
// element its surroundings resonate with most.
//   - use with essence: load it (up to 64 of one kind)
//   - use with an empty hand: show its resonance, or start a careful weave if it's loaded
//   - sneak + use with an empty hand: quick weave
// ELEMENT is the strongest element at the last reading; it only drives the particles.
public class LeyAltarBlock extends Block implements EntityBlock {
    public static final MapCodec<LeyAltarBlock> CODEC = simpleCodec(LeyAltarBlock::new);
    public static final EnumProperty<LeyElement> ELEMENT = EnumProperty.create("element", LeyElement.class);
    private static final VoxelShape SHAPE = Shapes.or(Block.box(1, 0, 1, 15, 4, 15), Block.box(4, 4, 4, 12, 11, 12), Block.box(2, 11, 2, 14, 14, 14), Block.box(6, 14, 6, 10, 16, 10));

    public LeyAltarBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(ELEMENT, LeyElement.ARCANE));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ELEMENT);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LeyAltarBlockEntity(pos, state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide()) LeyWeaving.refresh(level, pos);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (LeyWeaving.essence(stack) == null) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!(player instanceof ServerPlayer serverPlayer) || !(level.getBlockEntity(pos) instanceof LeyAltarBlockEntity altar)) {
            return InteractionResult.SUCCESS;
        }
        if (!altar.isOwner(player)) {
            LeyWeaving.status(serverPlayer, Component.translatable("message.skillcraft.altar_owned", altar.ownerName()), ChatFormatting.RED);
            return InteractionResult.SUCCESS;
        }
        if (!altar.accepts(stack.getItem())) {
            LeyWeaving.status(serverPlayer, Component.translatable("message.skillcraft.ley_full"), ChatFormatting.RED);
            return InteractionResult.SUCCESS;
        }
        int used = altar.deposit(player, stack.getItem(), stack.getCount());
        if (!player.isCreative()) stack.shrink(used);
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.6f, 1.2f);
        Resonance.Reading reading = LeyWeaving.refresh(level, pos);
        LeyWeaving.status(serverPlayer, Component.translatable("message.skillcraft.ley_loaded", altar.count(), altar.essence().getDefaultInstance().getHoverName(),
                LeyWeaving.moteName(reading.dominant()), LeyWeaving.element(reading.dominant(), reading.strength())), ChatFormatting.LIGHT_PURPLE);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown()) LeyWeaving.quickWeave(serverPlayer, pos);
        else LeyWeaving.openCareful(serverPlayer, pos);
        return InteractionResult.SUCCESS;
    }

    // A loaded altar can only be broken by its owner (or in creative)
    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        if (!player.isCreative() && level.getBlockEntity(pos) instanceof LeyAltarBlockEntity altar && !altar.isOwner(player)) {
            return 0f;
        }
        return super.getDestroyProgress(state, player, level, pos);
    }

    // The essence isn't kept in the dropped altar item, so hand it back when the altar is broken
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof LeyAltarBlockEntity altar && !altar.isEmpty()) {
            Block.popResource(level, pos, altar.contents());
            altar.clear();
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    // Motes of the altar's element drift up from the plate
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) != 0) return;
        double x = pos.getX() + 0.25 + random.nextDouble() * 0.5;
        double z = pos.getZ() + 0.25 + random.nextDouble() * 0.5;
        level.addParticle(state.getValue(ELEMENT).particle(), x, pos.getY() + 0.95, z, 0, 0.03, 0);
    }
}
