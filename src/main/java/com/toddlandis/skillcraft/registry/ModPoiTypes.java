package com.toddlandis.skillcraft.registry;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

import com.toddlandis.skillcraft.SkillCraft;
import com.toddlandis.skillcraft.node.NodeType;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

// Registers every node as a "point of interest" (the same index villagers use to find beds and
// job blocks). Minecraft keeps it up to date as nodes are placed or removed, which makes
// "how many oak nodes are within 8 blocks?" a cheap lookup.
public class ModPoiTypes {
    public static final DeferredRegister<PoiType> POI_TYPES = DeferredRegister.create(Registries.POINT_OF_INTEREST_TYPE, SkillCraft.MODID);

    public static final Map<NodeType, DeferredHolder<PoiType, PoiType>> NODES = new EnumMap<>(NodeType.class);

    static {
        for (NodeType type : NodeType.values()) {
            NODES.put(type, POI_TYPES.register(type.blockName(), () -> new PoiType(
                    Set.copyOf(ModBlocks.NODES.get(type).get().getStateDefinition().getPossibleStates()), 0, 1)));
        }
    }
}
