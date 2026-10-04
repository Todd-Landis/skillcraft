package com.toddlandis.skillcraft.ability;

import com.toddlandis.skillcraft.registry.ModDataComponents;
import com.toddlandis.skillcraft.registry.ModItems;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

// A Codex teaches one ability for good (spec 2.4). Which one is stored in the skillcraft:codex_ability
// component, so one item covers every Codex: "Codex of the Forgequake" is a codex with
// codex_ability = skillcraft:forgequake. Right-click to read it; it's used up. Tradeable.
public class CodexItem extends Item {
    public CodexItem(Properties properties) {
        super(properties);
    }

    public static Identifier ability(ItemStack stack) {
        return stack.get(ModDataComponents.CODEX_ABILITY.get());
    }

    public static ItemStack of(Identifier ability) {
        ItemStack stack = new ItemStack(ModItems.CODEX.get());
        stack.set(ModDataComponents.CODEX_ABILITY.get(), ability);
        return stack;
    }

    @Override
    public Component getName(ItemStack stack) {
        Identifier id = ability(stack);
        return id == null ? super.getName(stack) : Component.translatable("item.skillcraft.codex.of", AbilityDefinition.name(id));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.SUCCESS;
        Identifier id = ability(stack);
        AbilityDefinition def = AbilityManager.get(id);
        if (def == null || !def.requires().codex()) {
            serverPlayer.sendOverlayMessage(Component.translatable("message.skillcraft.codex_blank").withStyle(ChatFormatting.GRAY));
            return InteractionResult.FAIL;
        }
        AbilityData data = AbilityService.data(serverPlayer);
        if (!data.learn(id)) {
            serverPlayer.sendOverlayMessage(Component.translatable("message.skillcraft.codex_known", AbilityDefinition.name(id)).withStyle(ChatFormatting.GRAY));
            return InteractionResult.FAIL;
        }
        AbilityService.save(serverPlayer, data);
        if (!player.isCreative()) stack.shrink(1);
        serverPlayer.sendSystemMessage(Component.translatable("message.skillcraft.codex_learned", AbilityDefinition.name(id),
                def.requires().skill().displayName(), def.requires().level()).withStyle(ChatFormatting.GOLD));
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1f, 0.8f);
        AbilityService.announceUnlock(serverPlayer, id);
        return InteractionResult.SUCCESS;
    }
}
