package com.toddlandis.skillcraft.cooking;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.toddlandis.skillcraft.registry.ModDataMaps;
import com.toddlandis.skillcraft.skill.XpTable;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

// What a raw food turns into at the Cooking Range. Stored as a data map on the raw item:
// data/skillcraft/data_maps/item/cooking.json
//   result    - the cooked food
//   level     - Cooking level needed to load it
//   xp        - Cooking XP per item cooked (burnt food gives none)
//   time      - ticks per item (20 = 1 second)
//   stop_burn - Cooking level at which it never burns
//
// Burn chance falls in a straight line from MAX_BURN at the required level to 0 at stop_burn,
// so every food has a stretch where you still lose some, like classic MMO cooking.
public record CookingRecipe(Item result, int level, double xp, int time, int stopBurn) {
    public static final double MAX_BURN = 0.5;

    public static final Codec<CookingRecipe> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("result").forGetter(CookingRecipe::result),
            Codec.intRange(1, XpTable.MAX_LEVEL).fieldOf("level").forGetter(CookingRecipe::level),
            Codec.DOUBLE.fieldOf("xp").forGetter(CookingRecipe::xp),
            Codec.intRange(1, 20 * 60 * 10).fieldOf("time").forGetter(CookingRecipe::time),
            Codec.intRange(1, XpTable.MAX_LEVEL).fieldOf("stop_burn").forGetter(CookingRecipe::stopBurn)
    ).apply(instance, CookingRecipe::new));

    public double burnChance(int cookingLevel) {
        if (cookingLevel >= stopBurn) return 0;
        double span = Math.max(1, stopBurn - level);
        return Mth.clamp(MAX_BURN * (stopBurn - cookingLevel) / span, 0, MAX_BURN);
    }

    public static CookingRecipe get(ItemStack stack) {
        return stack.isEmpty() ? null : stack.typeHolder().getData(ModDataMaps.COOKING);
    }
}
