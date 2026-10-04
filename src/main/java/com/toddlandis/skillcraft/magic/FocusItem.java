package com.toddlandis.skillcraft.magic;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

// Wands and staves. Right-click in the main hand fires a bolt (MagicCasting); their numbers live
// in data_maps/item/magic_focus.json, so new foci need only a data entry and this item class.
public class FocusItem extends Item {
    public FocusItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(stack)) return InteractionResult.FAIL;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        return MagicCasting.castBolt((ServerPlayer) player, stack) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
    }
}
