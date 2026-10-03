package com.toddlandis.skillcraft.node;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

import com.mojang.serialization.Codec;
import com.toddlandis.skillcraft.altar.minigame.MinigameKind;
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
//   (depletion chance is calculated from tier and level, see depleteChance())
//   (regrow time and placement spacing are set by tier, see the tables below)
public enum NodeType implements StringRepresentable {
    // --- Woodcutting ---
    OAK(Skill.WOODCUTTING, 1, 25, () -> Items.OAK_LOG, 1, 32),
    BIRCH(Skill.WOODCUTTING, 1, 27.5, () -> Items.BIRCH_LOG, 1, 32),
    SPRUCE(Skill.WOODCUTTING, 1, 30, () -> Items.SPRUCE_LOG, 1, 32),
    JUNGLE(Skill.WOODCUTTING, 15, 50, () -> Items.JUNGLE_LOG, 1, 32),
    ACACIA(Skill.WOODCUTTING, 15, 52.5, () -> Items.ACACIA_LOG, 1, 32),
    MANGROVE(Skill.WOODCUTTING, 15, 55, () -> Items.MANGROVE_LOG, 1, 32),
    CHERRY(Skill.WOODCUTTING, 30, 85, () -> Items.CHERRY_LOG, 1, 48),
    DARK_OAK(Skill.WOODCUTTING, 30, 90, () -> Items.DARK_OAK_LOG, 1, 48),
    PALE_OAK(Skill.WOODCUTTING, 45, 135, () -> Items.PALE_OAK_LOG, 1, 48),
    CRIMSON(Skill.WOODCUTTING, 60, 200, () -> Items.CRIMSON_STEM, 1, 64),
    WARPED(Skill.WOODCUTTING, 60, 200, () -> Items.WARPED_STEM, 1, 64),

    // --- Mining ---
    COAL(Skill.MINING, 1, 17.5, () -> Items.COAL, 1, 32),
    COPPER(Skill.MINING, 1, 17.5, () -> Items.RAW_COPPER, 1, 32),
    IRON(Skill.MINING, 15, 35, () -> Items.RAW_IRON, 1, 32),
    QUARTZ(Skill.MINING, 15, 37.5, () -> Items.QUARTZ, 1, 32),
    REDSTONE(Skill.MINING, 30, 60, () -> Items.REDSTONE, 2, 64),
    GOLD(Skill.MINING, 30, 65, () -> Items.RAW_GOLD, 1, 24),
    LAPIS(Skill.MINING, 30, 62.5, () -> Items.LAPIS_LAZULI, 2, 48),
    DIAMOND(Skill.MINING, 45, 100, () -> Items.DIAMOND, 1, 16),
    EMERALD(Skill.MINING, 60, 140, () -> Items.EMERALD, 1, 16),
    ANCIENT_DEBRIS(Skill.MINING, 75, 200, () -> Items.ANCIENT_DEBRIS, 1, 8);

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

    NodeType(Skill skill, int level, double xp, Supplier<Item> drop, int dropCount, int inputCount) {
        this.skill = skill;
        this.level = level;
        this.xp = xp;
        this.drop = drop;
        this.dropCount = dropCount;
        this.inputCount = inputCount;
    }

    public Skill skill() { return skill; }
    public int level() { return level; }
    public double xp() { return xp; }
    public Item drop() { return drop.get(); }
    public int dropCount() { return dropCount; }
    // Nodes are attuned from the same resource they produce
    public Item input() { return drop.get(); }
    public int inputCount() { return inputCount; }
    public int tierIndex() { return XpTable.tierIndex(level); }
    // Change per node here if a specific node should use a different minigame
    public MinigameKind minigame() {
        return skill == Skill.MINING ? MinigameKind.TEMPER_THE_VEIN : MinigameKind.STRIKE_THE_GRAIN;
    }
    public String tierName() { return XpTable.tierName(level); }

    // Success chance per attempt: grows with level, multiplied by tool power (see GatheringSessions)
    public double successChance(int playerLevel, double toolPower) {
        int tier = tierIndex();
        double base = CHANCE_AT_LEVEL[tier] + (CHANCE_AT_99[tier] - CHANCE_AT_LEVEL[tier]) * levelProgress(playerLevel);
        return Math.min(0.95, base * toolPower);
    }

    // Chance that a successful attempt depletes the node. Depends only on level (tools affect
    // speed instead, see GatheringSessions):
    //   deplete = 1 - (1 - floor) * progress
    //   progress - 0 at the node's required level, 1 at level 99
    //   floor    - chance at level 99 = 25% + 12% per tier -> I 25%, II 37% ... VI 85%, VII 97%
    // At the required level a node always depletes after one success, and high-tier nodes
    // stay short-lived even for maxed players.
    private static final double DEPLETE_FLOOR_BASE = 0.25;
    private static final double DEPLETE_FLOOR_PER_TIER = 0.12;

    public double depleteChance(int playerLevel) {
        double floor = Math.min(1.0, DEPLETE_FLOOR_BASE + DEPLETE_FLOOR_PER_TIER * tierIndex());
        return 1.0 - (1.0 - floor) * levelProgress(playerLevel);
    }

    // Per-tier limits that keep high-tier resources scarce. A player can only work one node at a
    // time, so long regrow times mean needing many nodes, and spacing makes those nodes cost
    // land and walking time.
    //                                          I    II   III  IV   V    VI   VII
    private static final int[] REGROW_SECONDS = {20,  45,  120, 300, 480, 720, 900};
    // At most SPACING_MAX nodes of the same type within SPACING_RADIUS blocks
    private static final int[] SPACING_RADIUS = {8,   12,  16,  24,  32,  48,  64};
    private static final int[] SPACING_MAX =    {6,   4,   3,   2,   1,   1,   1};
    // Gathering fatigue (see GatheringFatigue): how many of this resource a player can gather
    // per hour, sustained, at the node's required level. Rises to 3x by level 99.
    private static final int[] FATIGUE_BASE_PER_HOUR = {400, 200, 100, 40, 20, 8, 4};
    private static final double FATIGUE_LEVEL_SCALING = 2.0; // +200% at 99
    private static final double FATIGUE_BURST_HOURS = 0.25;  // can bank 15 minutes' worth

    public int regrowSeconds() { return REGROW_SECONDS[tierIndex()]; }
    public int respawnTicks() { return regrowSeconds() * 20; }
    public int spacingRadius() { return SPACING_RADIUS[tierIndex()]; }
    public int spacingMax() { return SPACING_MAX[tierIndex()]; }

    // Sustained gathering rate for a player at this level
    public double fatiguePerHour(int playerLevel) {
        return FATIGUE_BASE_PER_HOUR[tierIndex()] * (1 + FATIGUE_LEVEL_SCALING * levelProgress(playerLevel));
    }

    // How much can be gathered in a burst before fatigue blocks gathering
    public double fatigueCapacity(int playerLevel) {
        return Math.max(2, fatiguePerHour(playerLevel) * FATIGUE_BURST_HOURS);
    }

    // 0 at the node's required level, 1 at level 99
    private double levelProgress(int playerLevel) {
        return Math.clamp((playerLevel - level) / (double) Math.max(1, XpTable.MAX_LEVEL - level), 0, 1);
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
