package com.toddlandis.skillcraft.network;

import com.toddlandis.skillcraft.SkillCraft;
import com.toddlandis.skillcraft.combat.AttackStyle;
import com.toddlandis.skillcraft.registry.ModAttachments;

import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

// Client -> server: the attack style key was pressed; switch to the next style.
public record CycleAttackStylePayload() implements CustomPacketPayload {
    public static final Type<CycleAttackStylePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(SkillCraft.MODID, "cycle_attack_style"));
    public static final StreamCodec<ByteBuf, CycleAttackStylePayload> STREAM_CODEC = StreamCodec.unit(new CycleAttackStylePayload());

    public static void handle(CycleAttackStylePayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        AttackStyle next = player.getData(ModAttachments.ATTACK_STYLE).next();
        player.setData(ModAttachments.ATTACK_STYLE, next);
        player.sendOverlayMessage(Component.translatable("message.skillcraft.attack_style", next.displayName().copy().withStyle(ChatFormatting.GOLD), next.description())
                .withStyle(ChatFormatting.YELLOW));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
