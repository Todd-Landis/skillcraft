package com.toddlandis.skillcraft.node;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

import com.mojang.serialization.Codec;
import com.toddlandis.skillcraft.skill.Skill;
import com.toddlandis.skillcraft.skill.XpTable;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

// Every skilling node in the mod. A node is the main way to train a gathering skill:
// full XP per successful attempt, but items come slowly and the node depletes and regrows.
//
//   level    - level needed to use it (and to attune it at the Attunement Altar)
//   xp       - XP per successful attempt. Breaking the matching block in the world gives 1% of this
//   drop     - item given per success, and how many
//   input    - resource used to create the node at the altar, and how many
//   deplete  - chance per success that the node is used up (stump / empty rock)
//   respawn  - seconds until a depleted node regrows
public enum NodeType implements StringRepresentable {
    // --- Woodcutting ---
    OAK(Skill.WOODCUTTING, 1, 25, () -> Items.OAK_LOG, 1, 32, 1 / 3.0, 20),
    BIRCH(Skill.WOODCUTTING, 1, 27.5, () -> Items.BIRCH_LOG, 1, 32, 1 / 3.0, 20),
    SPRUCE(Skill.WOODCUTTING, 1, 30, () -> Items.SPRUCE_LOG, 1, 32, 1 / 3.0, 20),
    JUNGLE(Skill.WOODCUTTING, 15, 50, () -> Items.JUNGLE_LOG, 1, 32, 1 / 4.0, 30),
    ACACIA(Skill.WOODCUTTING, 15, 52.5, () -> Items.ACACIA_LOG, 1, 32, 1 / 4.0, 30),
    MANGROVE(Skill.WOODCUTTING, 15, 55, () -> Items.MANGROVE_LOG, 1, 32, 1 / 4.0, 30),
    CHERRY(Skill.WOODCUTTING, 30, 85, () -> Items.CHERRY_LOG, 1, 48, 1 / 4.0, 45),
    DARK_OAK(Skill.WOODCUTTING, 30, 90, () -> Items.DARK_OAK_LOG, 1, 48, 1 / 4.0, 45),
    PALE_OAK(Skill.WOODCUTTING, 45, 135, () -> Items.PALE_OAK_LOG, 1, 48, 1 / 5.0, 60),
    CRIMSON(Skill.WOODCUTTING, 60, 200, () -> Items.CRIMSON_STEM, 1, 64, 1 / 5.0, 90),
    WARPED(Skill.WOODCUTTING, 60, 200, () -> Items.WARPED_STEM, 1, 64, 1 / 5.0, 90),

    // --- Mining ---
    COAL(Skill.MINING, 1, 17.5, () -> Items.COAL, 1, 32, 1 / 3.0, 20),
    COPPER(Skill.MINING, 1, 17.5, () -> Items.RAW_COPPER, 1, 32, 1 / 3.0, 20),
    IRON(Skill.MINING, 15, 35, () -> Items.RAW_IRON, 1, 32, 1 / 4.0, 30),
    QUARTZ(Skill.MINING, 15, 37.5, () -> Items.QUARTZ, 1, 32, 1 / 4.0, 30),
    REDSTONE(Skill.MINING, 30, 60, () -> Items.REDSTONE, 2, 64, 1 / 4.0, 45),
    GOLD(Skill.MINING, 30, 65, () -> Items.RAW_GOLD, 1, 24, 1 / 4.0, 45),
    LAPIS(Skill.MINING, 30, 62.5, () -> Items.LAPIS_LAZULI, 2, 48, 1 / 4.0, 45),
    DIAMOND(Skill.MINING, 45, 100, () -> Items.DIAMOND, 1, 16, 1 / 5.0, 90),
    EMERALD(Skill.MINING, 60, 140, () -> Items.EMERALD, 1, 16, 1 / 5.0, 120),
    ANCIENT_DEBRIS(Skill.MINING, 75, 200, () -> Items.ANCIENT_DEBRIS, 1, 8, 1 / 6.0, 180);

    public static final Codec<NodeType> CODEC = StringRepresentable.fromEnum(NodeType::values);

    // Base success chance per attempt with a stone tool, at the node's level -> at level 99, by tier
    private static final double[] CHANCE_AT_LEVEL = {0.40, 0.30, 0.22, 0.16, 0.12, 0.09, 0.07};
    private static final double[] CHANCE_AT_99 = {0.95, 0.85, 0.75, 0.65, 0.55, 0.48, 0.42};

    private final Skill skill;
    private final int level;
    private final double xp;
    private final Supplier<Item> drop;
    private final int dropCount;
    private final int inputCount;
    private final double depleteChance;
    private final int respawnSeconds;

    NodeType(Skill skill, int level, double xp, Supplier<Item> drop, int dropCount, int inputCount, double depleteChance, int respawnSeconds) {
        this.skill = skill;
        this.level = level;
        this.xp = xp;
        this.drop = drop;
        this.dropCount = dropCount;
        this.inputCount = inputCount;
        this.depleteChance = depleteChance;
        this.respawnSeconds = respawnSeconds;
    }

    public Skill skill() { return skill; }
    public int level() { return level; }
    public double xp() { return xp; }
    public Item drop() { return drop.get(); }
    public int dropCount() { return dropCount; }
    // Nodes are attuned from the same resource they produce
    public Item input() { return drop.get(); }
    public int inputCount() { return inputCount; }
    public double depleteChance() { return depleteChance; }
    public int respawnTicks() { return respawnSeconds * 20; }
    public int tierIndex() { return XpTable.tierIndex(level); }
    // Change per node here if a specific node should use a different minigame
    public MinigameKind minigame() {
        return skill == Skill.MINING ? MinigameKind.TEMPER_THE_VEIN : MinigameKind.STRIKE_THE_GRAIN;
    }
    public String tierName() { return XpTable.tierName(level); }

    // Success chance per attempt: grows with level, multiplied by tool power (see GatheringSessions)
    public double successChance(int playerLevel, double toolPower) {
        int tier = tierIndex();
        double progress = (playerLevel - level) / (double) Math.max(1, XpTable.MAX_LEVEL - level);
        double base = CHANCE_AT_LEVEL[tier] + (CHANCE_AT_99[tier] - CHANCE_AT_LEVEL[tier]) * Math.clamp(progress, 0, 1);
        return Math.min(0.95, base * toolPower);
    }

    public String blockName() {
        return getSerializedName() + "_node";
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    private static final Map<Item, NodeType> BY_INPUT = new HashMap<>();

    // The node a resource item can be attuned into, or null
    public static NodeType byInput(Item item) {
        if (BY_INPUT.isEmpty()) {
            for (NodeType type : values()) BY_INPUT.put(type.input(), type);
        }
        return BY_INPUT.get(item);
    }
}
