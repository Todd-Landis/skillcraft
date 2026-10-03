package com.toddlandis.skillcraft.network;

import com.toddlandis.skillcraft.SkillCraft;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

// Server -> client: an ability was used (or started winding up, or a toggle flipped), so the HUD
// can draw its cooldown sweep, the global cooldown and the windup bar. Times are in ticks from now.
public record AbilityCooldownPayload(Identifier ability, int cooldown, int globalCooldown, int windup, boolean toggledOn) implements CustomPacketPayload {
    public static final Type<AbilityCooldownPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(SkillCraft.MODID, "ability_cooldown"));
    public static final StreamCodec<ByteBuf, AbilityCooldownPayload> STREAM_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, AbilityCooldownPayload::ability,
            ByteBufCodecs.VAR_INT, AbilityCooldownPayload::cooldown,
            ByteBufCodecs.VAR_INT, AbilityCooldownPayload::globalCooldown,
            ByteBufCodecs.VAR_INT, AbilityCooldownPayload::windup,
            ByteBufCodecs.BOOL, AbilityCooldownPayload::toggledOn,
            AbilityCooldownPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
