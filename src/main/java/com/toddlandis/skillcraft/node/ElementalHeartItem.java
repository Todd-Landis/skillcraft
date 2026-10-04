package com.toddlandis.skillcraft.node;

import com.toddlandis.skillcraft.registry.ModDataComponents;
import com.toddlandis.skillcraft.registry.ModItems;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

// An Elemental Heart: the seed of a skilling node, found in the ordinary Minecraft world (felling
// trees, mining ores, fishing with a rod; see HeartEvents). The Attunement Altar turns one into
// the matching node. One item for every node type; which one is in its skillcraft:heart_node
// component, and the client tints it with the node's colour (client/HeartTint).
public class ElementalHeartItem extends Item {
    public ElementalHeartItem(Properties properties) {
        super(properties);
    }

    public static ItemStack of(NodeType type) {
        ItemStack stack = new ItemStack(ModItems.ELEMENTAL_HEART.get());
        stack.set(ModDataComponents.HEART_NODE.get(), type);
        return stack;
    }

    // The node this stack attunes into, or null if it isn't a heart (or has no node)
    public static NodeType node(ItemStack stack) {
        return stack.isEmpty() ? null : stack.get(ModDataComponents.HEART_NODE.get());
    }

    // "Oak Heart", "Diamond Heart", "Cod Heart"
    @Override
    public Component getName(ItemStack stack) {
        NodeType type = node(stack);
        return type == null ? super.getName(stack) : Component.translatable("item.skillcraft.elemental_heart.of", Component.translatable(type.heartKey()));
    }
}
