package com.toddlandis.skillcraft.prayer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

// Data map value (data_maps/item/prayer_offerings.json): the Prayer XP for burying one of this
// item. Offering it at a Votive Shrine pays `xp` times the shrine multiplier (server config).
public record PrayerOffering(double xp) {
    public static final Codec<PrayerOffering> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.doubleRange(0, 10000).fieldOf("xp").forGetter(PrayerOffering::xp)
    ).apply(i, PrayerOffering::new));
}
