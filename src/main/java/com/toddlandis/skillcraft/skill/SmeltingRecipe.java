package com.toddlandis.skillcraft.skill;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

// What a raw material turns into at the Smelter. Stored as a data map on the input item:
// data/skillcraft/data_maps/item/smelting.json
//   result/count - output per input item
//   coal         - coal or charcoal used per item
//   level        - Smithing level needed to load it
//   xp           - Smithing XP per item
//   time         - ticks per item (20 = 1 second)
public record SmeltingRecipe(Item result, int count, int coal, int level, double xp, int time) {
    public static final Codec<SmeltingRecipe> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("result").forGetter(SmeltingRecipe::result),
            Codec.intRange(1, 64).optionalFieldOf("count", 1).forGetter(SmeltingRecipe::count),
            Codec.intRange(0, 64).fieldOf("coal").forGetter(SmeltingRecipe::coal),
            Codec.intRange(1, XpTable.MAX_LEVEL).fieldOf("level").forGetter(SmeltingRecipe::level),
            Codec.DOUBLE.fieldOf("xp").forGetter(SmeltingRecipe::xp),
            Codec.intRange(1, 20 * 60 * 10).fieldOf("time").forGetter(SmeltingRecipe::time)
    ).apply(instance, SmeltingRecipe::new));
}
