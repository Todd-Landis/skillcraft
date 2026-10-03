package com.toddlandis.skillcraft.network;

import com.toddlandis.skillcraft.SkillCraft;
import com.toddlandis.skillcraft.bench.BenchMenu;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

// Client -> server: queue `count` of `item` at the open bench, or cancel (count = 0).
// The server re-checks everything (open menu, recipe, level, ingredients).
public record BenchActionPayload(int containerId, Identifier item, int count) implements CustomPacketPayload {
    public static final Type<BenchActionPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(SkillCraft.MODID, "bench_action"));

    public static final StreamCodec<ByteBuf, BenchActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, BenchActionPayload::containerId,
            Identifier.STREAM_CODEC, BenchActionPayload::item,
            ByteBufCodecs.VAR_INT, BenchActionPayload::count,
            BenchActionPayload::new);

    public static BenchActionPayload cancel(int containerId) {
        return new BenchActionPayload(containerId, Identifier.withDefaultNamespace("air"), 0);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(BenchActionPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        if (!(player.containerMenu instanceof BenchMenu menu) || menu.containerId != payload.containerId() || menu.bench() == null) return;
        if (payload.count() <= 0) {
            menu.bench().cancel(player);
        } else {
            BuiltInRegistries.ITEM.getOptional(payload.item()).ifPresent(item -> menu.bench().queue(player, item, payload.count()));
        }
    }
}
