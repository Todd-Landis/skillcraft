package com.toddlandis.skillcraft.client;

import com.mojang.serialization.MapCodec;
import com.toddlandis.skillcraft.node.ElementalHeartItem;
import com.toddlandis.skillcraft.node.NodeType;

import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

// Item tint "skillcraft:heart": colours an Elemental Heart with its node's colour (NodeType.color),
// so one texture serves every heart. Used by assets/skillcraft/items/elemental_heart.json.
public final class HeartTint implements ItemTintSource {
    public static final HeartTint INSTANCE = new HeartTint();
    public static final MapCodec<HeartTint> MAP_CODEC = MapCodec.unit(INSTANCE);

    private HeartTint() {}

    @Override
    public int calculate(ItemStack stack, ClientLevel level, LivingEntity entity) {
        NodeType type = ElementalHeartItem.node(stack);
        return type == null ? -1 : 0xFF000000 | type.color();
    }

    @Override
    public MapCodec<? extends ItemTintSource> type() {
        return MAP_CODEC;
    }
}
