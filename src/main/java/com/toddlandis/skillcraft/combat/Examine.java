package com.toddlandis.skillcraft.combat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

import com.toddlandis.skillcraft.SkillCraft;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

// Sneak + right-click a mob with an empty hand to see what it is weak to and what it resists,
// so players can learn the combat triangle without a wiki.
@EventBusSubscriber(modid = SkillCraft.MODID)
public final class Examine {
    private static final Map<UUID, Long> LAST = new WeakHashMap<>();

    private Examine() {}

    @SubscribeEvent
    static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getHand() != InteractionHand.MAIN_HAND || !event.getEntity().isShiftKeyDown()) return;
        if (!event.getEntity().getMainHandItem().isEmpty() || !(event.getTarget() instanceof Mob target)) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        long now = player.level().getGameTime();
        Long last = LAST.get(player.getUUID());
        if (last != null && now - last < 10) return;
        LAST.put(player.getUUID(), now);
        for (Component line : describe(target)) player.sendSystemMessage(line);
    }

    public static List<Component> describe(LivingEntity target) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("message.skillcraft.examine", target.getDisplayName()).withStyle(ChatFormatting.GOLD));
        MutableComponent weak = Component.empty(), resist = Component.empty();
        boolean anyWeak = false, anyResist = false;
        for (DamageStyle style : DamageStyle.values()) {
            double value = DamageStyles.modifier(target, style);
            String percent = " " + Math.round(value * 100) + "%";
            if (value >= CombatDamage.EFFECTIVE) {
                if (anyWeak) weak.append(", ");
                weak.append(style.displayName()).append(Component.literal(percent).withStyle(ChatFormatting.GRAY));
                anyWeak = true;
            } else if (value <= CombatDamage.RESISTED) {
                if (anyResist) resist.append(", ");
                resist.append(style.displayName()).append(Component.literal(percent).withStyle(ChatFormatting.GRAY));
                anyResist = true;
            }
        }
        lines.add(Component.translatable("message.skillcraft.examine_weak", anyWeak ? weak : Component.translatable("message.skillcraft.examine_none")).withStyle(ChatFormatting.GREEN));
        lines.add(Component.translatable("message.skillcraft.examine_resist", anyResist ? resist : Component.translatable("message.skillcraft.examine_none")).withStyle(ChatFormatting.RED));
        lines.add(Component.translatable("message.skillcraft.examine_attack", DamageStyles.profile(target.getType()).attackStyle().group().displayName()).withStyle(ChatFormatting.GRAY));
        return lines;
    }
}
