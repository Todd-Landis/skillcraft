package com.toddlandis.skillcraft.altar;

import com.mojang.serialization.MapCodec;
import com.toddlandis.skillcraft.network.OpenAttunementPayload;
import com.toddlandis.skillcraft.node.NodeType;
import com.toddlandis.skillcraft.skill.SkillManager;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.network.PacketDistributor;

// The Attunement Altar turns a pile of resources into a skilling node.
//   1. Right-click with the resource (e.g. oak logs) to load it.
//   2. Once full, right-click with an empty hand to start the attunement minigame.
//   3. Sneak + right-click with an empty hand to take the materials back out.
public class AttunementAltarBlock extends Block implements EntityBlock {
    public static final MapCodec<AttunementAltarBlock> CODEC = simpleCodec(AttunementAltarBlock::new);

    public AttunementAltarBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AttunementAltarBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        NodeType type = NodeType.byInput(stack.getItem());
        if (type == null) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!(player instanceof ServerPlayer serverPlayer) || !(level.getBlockEntity(pos) instanceof AttunementAltarBlockEntity altar)) {
            return InteractionResult.SUCCESS;
        }

        if (!altar.isEmpty() && altar.target() != type) {
            status(serverPlayer, Component.translatable("message.skillcraft.altar_busy", nodeName(altar.target())), ChatFormatting.RED);
            return InteractionResult.SUCCESS;
        }
        if (SkillManager.level(player, type.skill()) < type.level()) {
            status(serverPlayer, Component.translatable("message.skillcraft.resource_level", type.skill().displayName(), type.level()), ChatFormatting.RED);
            return InteractionResult.SUCCESS;
        }
        if (altar.isFull()) {
            status(serverPlayer, Component.translatable("message.skillcraft.altar_ready"), ChatFormatting.AQUA);
            return InteractionResult.SUCCESS;
        }

        int used = altar.deposit(type, stack.getCount());
        if (!player.isCreative()) stack.shrink(used);
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.5f, 0.6f + 0.8f * altar.stored() / type.inputCount());
        sendProgress(serverPlayer, altar);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(player instanceof ServerPlayer serverPlayer) || !(level.getBlockEntity(pos) instanceof AttunementAltarBlockEntity altar)) {
            return InteractionResult.SUCCESS;
        }
        if (altar.isEmpty()) {
            status(serverPlayer, Component.translatable("message.skillcraft.altar_empty"), ChatFormatting.GRAY);
        } else if (player.isShiftKeyDown()) {
            ItemStack refund = new ItemStack(altar.target().input(), altar.stored());
            altar.clear();
            if (!player.getInventory().add(refund)) player.drop(refund, false);
            status(serverPlayer, Component.translatable("message.skillcraft.altar_withdrawn"), ChatFormatting.GRAY);
        } else if (altar.isFull()) {
            PacketDistributor.sendToPlayer(serverPlayer, new OpenAttunementPayload(pos, altar.target()));
        } else {
            sendProgress(serverPlayer, altar);
        }
        return InteractionResult.SUCCESS;
    }

    // Material isn't kept inside the dropped altar item, so give it back when the altar is broken
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof AttunementAltarBlockEntity altar && !altar.isEmpty()) {
            Block.popResource(level, pos, new ItemStack(altar.target().input(), altar.stored()));
            altar.clear();
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    private static void sendProgress(ServerPlayer player, AttunementAltarBlockEntity altar) {
        NodeType type = altar.target();
        Component text = altar.isFull()
                ? Component.translatable("message.skillcraft.altar_ready")
                : Component.translatable("message.skillcraft.altar_progress", nodeName(type), altar.stored(), type.inputCount(), type.input().getDefaultInstance().getHoverName());
        status(player, text, altar.isFull() ? ChatFormatting.AQUA : ChatFormatting.YELLOW);
    }

    private static void status(ServerPlayer player, Component text, ChatFormatting color) {
        player.sendOverlayMessage(text.copy().withStyle(color));
    }

    public static Component nodeName(NodeType type) {
        return Component.translatable("block.skillcraft." + type.blockName());
    }
}
