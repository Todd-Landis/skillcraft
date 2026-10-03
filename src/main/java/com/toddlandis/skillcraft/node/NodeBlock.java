package com.toddlandis.skillcraft.node;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.toddlandis.skillcraft.registry.ModBlocks;
import com.toddlandis.skillcraft.registry.ModItems;
import com.toddlandis.skillcraft.registry.ModSounds;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

// A placed skilling node. Right-click with the right tool to start gathering (see GatheringSessions).
// Can't be broken normally; use the Relocator to pick it up.
public class NodeBlock extends Block {
    public static final BooleanProperty DEPLETED = BooleanProperty.create("depleted");

    public static final MapCodec<NodeBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            NodeType.CODEC.fieldOf("node").forGetter(NodeBlock::type),
            propertiesCodec()
    ).apply(instance, NodeBlock::new));

    private static final VoxelShape BASE = Block.box(0, 0, 0, 16, 3, 16);
    private static final VoxelShape FULL_SHAPE = Shapes.or(BASE, Block.box(2, 3, 2, 14, 15, 14));
    private static final VoxelShape DEPLETED_SHAPE = Shapes.or(BASE, Block.box(3, 3, 3, 13, 8, 13));

    private final NodeType type;

    public NodeBlock(NodeType type, Properties properties) {
        super(properties);
        this.type = type;
        registerDefaultState(stateDefinition.any().setValue(DEPLETED, false));
    }

    public NodeType type() {
        return type;
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DEPLETED);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(DEPLETED) ? DEPLETED_SHAPE : FULL_SHAPE;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.is(ModItems.RELOCATOR.get())) {
            if (level instanceof ServerLevel serverLevel) relocate(serverLevel, pos, player);
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            GatheringSessions.start(serverPlayer, pos, this, state);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer serverPlayer) {
            GatheringSessions.start(serverPlayer, pos, this, state);
        }
        return InteractionResult.SUCCESS;
    }

    // Picks the node up as an item so it can be moved
    private void relocate(ServerLevel level, BlockPos pos, Player player) {
        level.removeBlock(pos, false);
        ItemStack item = new ItemStack(ModBlocks.NODES.get(type).get());
        if (!player.getInventory().add(item)) player.drop(item, false);
        level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.8f, 0.8f);
        player.sendOverlayMessage(Component.translatable("message.skillcraft.node_relocated").withStyle(ChatFormatting.GRAY));
    }

    // Called by GatheringSessions when a success roll depletes the node
    public void deplete(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state.setValue(DEPLETED, true), Block.UPDATE_ALL);
        level.scheduleTick(pos, this, type.respawnTicks());
    }

    // Scheduled tick: the node regrows
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(DEPLETED)) {
            level.setBlock(pos, state.setValue(DEPLETED, false), Block.UPDATE_ALL);
            level.playSound(null, pos, ModSounds.NODE_REGROW.get(), SoundSource.BLOCKS, 0.8f, 1.0f);
        }
    }
}
