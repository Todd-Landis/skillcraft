package com.toddlandis.skillcraft.bonding;

import com.toddlandis.skillcraft.network.ModNetwork;
import com.toddlandis.skillcraft.network.OpenCodexPayload;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

// The Bond Codex (docs/BEASTBONDING.md): use it on a creature to bond it (handled in
// BondEvents.onInteract, before the creature's own interaction), or in the air to open the codex
// screen (also on key H). The bonds themselves live on the player, not on this book.
public class BondCodexItem extends Item {
    public BondCodexItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer serverPlayer) ModNetwork.sendTo(serverPlayer, OpenCodexPayload.INSTANCE);
        return InteractionResult.SUCCESS;
    }
}
