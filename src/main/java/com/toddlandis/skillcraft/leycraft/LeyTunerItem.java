package com.toddlandis.skillcraft.leycraft;

import com.toddlandis.skillcraft.Config;
import com.toddlandis.skillcraft.registry.ModDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

// The Ley Tuner, a tuning fork for finding where to build a Ley Altar (docs/LEYCRAFT.md section 7):
//   - use: ring it, and it points to the nearest strong source of its element (see LeyTuning)
//   - sneak + use: retune it to the next element
// The element is kept in the skillcraft:ley_tuning component, so each tuner remembers its own.
public class LeyTunerItem extends Item {
    public LeyTunerItem(Properties properties) {
        super(properties);
    }

    public static LeyElement element(ItemStack stack) {
        LeyElement element = stack.get(ModDataComponents.LEY_TUNING.get());
        return element == null ? LeyElement.ARCANE : element;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (level.isClientSide()) return InteractionResult.SUCCESS;
            LeyElement next = LeyElement.values()[(element(stack).ordinal() + 1) % LeyElement.values().length];
            stack.set(ModDataComponents.LEY_TUNING.get(), next);
            level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_STEP, SoundSource.PLAYERS, 1.0f, 1.0f + 0.2f * next.ordinal());
            player.sendOverlayMessage(Component.translatable("message.skillcraft.tuner_tuned", next.displayName()).withStyle(ChatFormatting.LIGHT_PURPLE));
            return InteractionResult.SUCCESS;
        }
        if (player.getCooldowns().isOnCooldown(stack)) return InteractionResult.FAIL;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        player.getCooldowns().addCooldown(stack, Config.LEY_TUNER_COOLDOWN.get());
        LeyTuning.ring((ServerPlayer) player, element(stack));
        return InteractionResult.SUCCESS;
    }

    // "Ley Tuner (Fire)"
    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.skillcraft.ley_tuner.tuned", super.getName(stack), element(stack).displayName());
    }
}
