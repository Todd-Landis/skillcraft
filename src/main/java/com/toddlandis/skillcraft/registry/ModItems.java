package com.toddlandis.skillcraft.registry;

import java.util.EnumMap;
import java.util.Map;

import com.toddlandis.skillcraft.SkillCraft;
import com.toddlandis.skillcraft.node.NodeType;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SkillCraft.MODID);

    public static final Map<NodeType, DeferredItem<BlockItem>> NODES = new EnumMap<>(NodeType.class);

    static {
        for (NodeType type : NodeType.values()) {
            NODES.put(type, ITEMS.registerSimpleBlockItem(type.blockName(), ModBlocks.NODES.get(type), p -> p.stacksTo(16)));
        }
    }

    public static final DeferredItem<BlockItem> ATTUNEMENT_ALTAR = ITEMS.registerSimpleBlockItem("attunement_altar", ModBlocks.ATTUNEMENT_ALTAR);

    // Right-click a node with this to pick it up
    public static final DeferredItem<Item> RELOCATOR = ITEMS.registerSimpleItem("relocator", p -> p.stacksTo(1));
}
