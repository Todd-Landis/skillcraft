package com.toddlandis.skillcraft.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

// Item component for gear won from a boss: which boss (a translation key) and who won it.
// Like "Made by", it marks the gear as earned by a player (see CraftedGear.isPlayerMade).
public record EarnedBy(String source, String player) {
    public static final Codec<EarnedBy> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("source").forGetter(EarnedBy::source),
            Codec.STRING.fieldOf("player").forGetter(EarnedBy::player)
    ).apply(instance, EarnedBy::new));

    public static final StreamCodec<ByteBuf, EarnedBy> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, EarnedBy::source,
            ByteBufCodecs.STRING_UTF8, EarnedBy::player,
            EarnedBy::new);
}
