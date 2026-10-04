package com.toddlandis.skillcraft.combat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

// Data map value: which damage type an item deals in melee, e.g. {"style": "crush"}.
// data/skillcraft/data_maps/item/weapon_styles.json. Unlisted items hit as crush.
public record WeaponStyle(DamageStyle style) {
    public static final Codec<WeaponStyle> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            DamageStyle.CODEC.fieldOf("style").forGetter(WeaponStyle::style)
    ).apply(instance, WeaponStyle::new));
}
