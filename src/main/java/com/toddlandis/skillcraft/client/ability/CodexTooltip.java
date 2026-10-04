package com.toddlandis.skillcraft.client.ability;

import java.util.List;

import com.toddlandis.skillcraft.ability.AbilityDefinition;
import com.toddlandis.skillcraft.ability.AbilityService;
import com.toddlandis.skillcraft.ability.CodexItem;
import com.toddlandis.skillcraft.registry.ModItems;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

// Codex tooltip: which ability it teaches, what that needs, and whether you already know it
public final class CodexTooltip {
    private CodexTooltip() {}

    public static void add(ItemStack stack, Player player, List<Component> tooltip) {
        if (!stack.is(ModItems.CODEX.get())) return;
        Identifier id = CodexItem.ability(stack);
        AbilityDefinition def = ClientAbilities.get(id);
        if (id == null || def == null) {
            tooltip.add(Component.translatable("tooltip.skillcraft.codex_blank").withStyle(ChatFormatting.GRAY));
            return;
        }
        tooltip.add(Component.translatable("tooltip.skillcraft.codex_teaches", AbilityDefinition.name(id).copy().withStyle(ChatFormatting.GOLD),
                def.category().displayName()).withStyle(ChatFormatting.GRAY));
        tooltip.add(AbilityDefinition.description(id).copy().withStyle(ChatFormatting.DARK_GRAY));
        Component needs = Component.translatable("tooltip.skillcraft.codex_needs", def.requires().skill().displayName(), def.requires().level());
        if (def.requires().weapon().isPresent()) needs = needs.copy().append(", ").append(AbilityService.weaponName(def));
        tooltip.add(needs.copy().withStyle(ChatFormatting.GRAY));
        boolean known = ClientAbilities.data(player).hasLearned(id);
        tooltip.add(Component.translatable(known ? "tooltip.skillcraft.codex_known" : "tooltip.skillcraft.codex_use")
                .withStyle(known ? ChatFormatting.GREEN : ChatFormatting.YELLOW));
    }
}
