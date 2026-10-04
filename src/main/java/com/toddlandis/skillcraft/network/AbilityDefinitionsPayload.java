package com.toddlandis.skillcraft.network;

import java.util.Map;

import com.mojang.serialization.Codec;
import com.toddlandis.skillcraft.SkillCraft;
import com.toddlandis.skillcraft.ability.AbilityDefinition;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

// Server -> client: every ability definition (on login and after /reload), for the HUD and Ability Book
public record AbilityDefinitionsPayload(Map<Identifier, AbilityDefinition> definitions) implements CustomPacketPayload {
    public static final Type<AbilityDefinitionsPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(SkillCraft.MODID, "ability_definitions"));
    public static final StreamCodec<ByteBuf, AbilityDefinitionsPayload> STREAM_CODEC = ByteBufCodecs.fromCodec(
            Codec.unboundedMap(Identifier.CODEC, AbilityDefinition.CODEC)).map(AbilityDefinitionsPayload::new, AbilityDefinitionsPayload::definitions);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
