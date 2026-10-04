package com.toddlandis.skillcraft.magic;

import java.util.Locale;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.toddlandis.skillcraft.combat.DamageStyle;
import com.toddlandis.skillcraft.registry.ModDataMaps;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

// Data map value for a wand or staff (data_maps/item/magic_focus.json):
//   kind: wand (quick, one-handed) or staff (harder hits, supplies its element's motes)
//   power: one bolt's hit; spells deal a share of it (like melee abilities and the weapon's hit)
//   element: the bolt's damage type (arcane if missing)
//   cast_cooldown: ticks between bolts
// See docs/AGILITY_MAGIC_PRAYER.md.
public record MagicFocus(Kind kind, float power, Optional<DamageStyle> element, int castCooldown) {
    public enum Kind implements StringRepresentable {
        WAND, STAFF;

        public static final Codec<Kind> CODEC = StringRepresentable.fromEnum(Kind::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final Codec<MagicFocus> CODEC = RecordCodecBuilder.create(i -> i.group(
            Kind.CODEC.fieldOf("kind").forGetter(MagicFocus::kind),
            Codec.floatRange(0, 100).fieldOf("power").forGetter(MagicFocus::power),
            DamageStyle.CODEC.validate(style -> style.isMagic() ? com.mojang.serialization.DataResult.success(style)
                    : com.mojang.serialization.DataResult.error(() -> "a focus element must be fire, frost, storm or arcane"))
                    .optionalFieldOf("element").forGetter(MagicFocus::element),
            Codec.intRange(2, 200).optionalFieldOf("cast_cooldown", 20).forGetter(MagicFocus::castCooldown)
    ).apply(i, MagicFocus::new));

    // The focus an item stack is, or null
    public static MagicFocus of(ItemStack stack) {
        return stack.isEmpty() ? null : stack.typeHolder().getData(ModDataMaps.MAGIC_FOCI);
    }

    // The focus in the player's main hand, or null (spells and bolts are cast from the main hand)
    public static MagicFocus held(Player player) {
        return of(player.getMainHandItem());
    }

    public DamageStyle elementOrArcane() {
        return element.orElse(DamageStyle.ARCANE);
    }

    // True if the main-hand focus is a staff of the element this mote belongs to (its casts are free)
    public static boolean supplies(Player player, Item mote) {
        MagicFocus focus = held(player);
        return focus != null && focus.kind == Kind.STAFF && focus.element.map(element -> Motes.of(element) == mote).orElse(false);
    }
}
