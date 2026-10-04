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
//   xp       - XP per successful attempt (world blocks give no XP; they can drop the node's heart)
//   drop     - item given per success, and how many
//   color    - its Elemental Heart's colour (nodes are attuned from hearts found in the vanilla
//              world, see ElementalHeartItem and docs/DESIGN.md "Elemental Hearts")
//   (depletion chance is calculated from tier and level, see depleteChance())
//   (regrow time and placement spacing are set by tier, see the tables below)
public enum NodeType implements StringRepresentable {
    // --- Woodcutting ---
    OAK(Skill.WOODCUTTING, 1, 25, () -> Items.OAK_LOG, 1, 0xB8945F),
    BIRCH(Skill.WOODCUTTING, 1, 27.5, () -> Items.BIRCH_LOG, 1, 0xE2DCB8),
    SPRUCE(Skill.WOODCUTTING, 1, 30, () -> Items.SPRUCE_LOG, 1, 0x7A5432),
    JUNGLE(Skill.WOODCUTTING, 10, 50, () -> Items.JUNGLE_LOG, 1, 0xB0804E),
    ACACIA(Skill.WOODCUTTING, 10, 52.5, () -> Items.ACACIA_LOG, 1, 0xD0703C),
    MANGROVE(Skill.WOODCUTTING, 10, 55, () -> Items.MANGROVE_LOG, 1, 0x9A3E38),
    CHERRY(Skill.WOODCUTTING, 20, 85, () -> Items.CHERRY_LOG, 1, 0xF2A6C2),
    DARK_OAK(Skill.WOODCUTTING, 20, 90, () -> Items.DARK_OAK_LOG, 1, 0x5A3C24),
    PALE_OAK(Skill.WOODCUTTING, 30, 135, () -> Items.PALE_OAK_LOG, 1, 0xDCD4CC),
    CRIMSON(Skill.WOODCUTTING, 40, 175, () -> Items.CRIMSON_STEM, 1, 0xB03E62),
    WARPED(Skill.WOODCUTTING, 40, 175, () -> Items.WARPED_STEM, 1, 0x2FB09A),

    // --- Mining ---
    COAL(Skill.MINING, 1, 17.5, () -> Items.COAL, 1, 0x4A4A4E),
    COPPER(Skill.MINING, 1, 17.5, () -> Items.RAW_COPPER, 1, 0xD87C4C),
    IRON(Skill.MINING, 10, 35, () -> Items.RAW_IRON, 1, 0xE0CCB8),
    QUARTZ(Skill.MINING, 10, 37.5, () -> Items.QUARTZ, 1, 0xF0EAE2),
    REDSTONE(Skill.MINING, 20, 60, () -> Items.REDSTONE, 2, 0xE52A2A),
    GOLD(Skill.MINING, 20, 65, () -> Items.RAW_GOLD, 1, 0xF7D44A),
    LAPIS(Skill.MINING, 20, 62.5, () -> Items.LAPIS_LAZULI, 2, 0x3A5CD8),
    DIAMOND(Skill.MINING, 30, 100, () -> Items.DIAMOND, 1, 0x60EAE4),
    EMERALD(Skill.MINING, 40, 140, () -> Items.EMERALD, 1, 0x34D866),
    ANCIENT_DEBRIS(Skill.MINING, 40, 175, () -> Items.ANCIENT_DEBRIS, 1, 0x8A5A46),

    // --- Fishing ---
    // Fishing spots aren't blocks: they are patches of churning water (fishing/FishingSpotEntity)
    // placed on open water from a "shoal" item. Depleting one makes the shoal swim off and
    // resurface somewhere else in the same pond after the regrow time.
    COD(Skill.FISHING, 1, 20, () -> Items.COD, 1, 0xC2AA80),
    SALMON(Skill.FISHING, 10, 45, () -> Items.SALMON, 1, 0xE8705A),
    TROPICAL_FISH(Skill.FISHING, 20, 80, () -> Items.TROPICAL_FISH, 1, 0xF89028),
    PUFFERFISH(Skill.FISHING, 30, 120, () -> Items.PUFFERFISH, 1, 0xEED84A);

    public static final Codec<NodeType> CODEC = StringRepresentable.fromEnum(NodeType::values);

    // Base success chance per attempt with a stone tool, at the node's level -> at level 99, by tier
    private static final double[] CHANCE_AT_LEVEL = {0.40, 0.33, 0.27, 0.22, 0.18, 0.15, 0.12, 0.10, 0.08, 0.07};
    private static final double[] CHANCE_AT_99 = {0.95, 0.88, 0.80, 0.73, 0.66, 0.60, 0.54, 0.49, 0.45, 0.42};

    private final Skill skill;
    private final int level;
    private final double xp;
    private final Supplier<Item> drop;
    private final int dropCount;
    private final int color;

    NodeType(Skill skill, int level, double xp, Supplier<Item> drop, int dropCount, int color) {
        this.skill = skill;
        this.level = level;
        this.xp = xp;
        this.drop = drop;
        this.dropCount = dropCount;
        this.color = color;
    }

    public Skill skill() { return skill; }
    public int level() { return level; }
    public double xp() { return xp; }
    public Item drop() { return drop.get(); }
    public int dropCount() { return dropCount; }
    // The heart's tint (RGB)
    public int color() { return color; }
    public int tierIndex() { return XpTable.tierIndex(level); }
    // Change per node here if a specific node should use a different minigame
    public MinigameKind minigame() {
        return switch (skill) {
            case MINING -> MinigameKind.TEMPER_THE_VEIN;
            case FISHING -> MinigameKind.SET_THE_HOOK;
            default -> MinigameKind.STRIKE_THE_GRAIN;
        };
    }

    // Fishing spots live on water as entities instead of being placed as blocks
    public boolean isSpot() { return skill == Skill.FISHING; }

    // Smallest pond (connected open-water tiles) a spot of this tier can sit in: 12, 18, 24, 30...
    public int minWaterArea() { return 12 + 6 * tierIndex(); }
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
    //   floor    - chance at level 99 = 25% + 8% per tier -> I 25%, II 33% ... V 57% ... X 97%
    // At the required level a node always depletes after one success, and high-tier nodes
    // stay short-lived even for maxed players.
    private static final double DEPLETE_FLOOR_BASE = 0.25;
    private static final double DEPLETE_FLOOR_PER_TIER = 0.08;

    public double depleteChance(int playerLevel) {
        double floor = Math.min(1.0, DEPLETE_FLOOR_BASE + DEPLETE_FLOOR_PER_TIER * tierIndex());
        return 1.0 - (1.0 - floor) * levelProgress(playerLevel);
    }

    // Per-tier limits that keep high-tier resources scarce. A player can only work one node at a
    // time, so long regrow times mean needing many nodes, and spacing makes those nodes cost
    // land and walking time.
    //                                          I    II   III  IV   V    VI   VII  VIII IX    X
    private static final int[] REGROW_SECONDS = {20,  45,  120, 300, 600, 720, 840, 960, 1080, 1200};
    // At most SPACING_MAX nodes of the same type within SPACING_RADIUS blocks
    private static final int[] SPACING_RADIUS = {8,   12,  16,  24,  40,  48,  56,  64,  72,   80};
    private static final int[] SPACING_MAX =    {6,   4,   3,   2,   1,   1,   1,   1,   1,    1};
    // Gathering fatigue (see GatheringFatigue): how many of this resource a player can gather
    // per hour, sustained, at the node's required level. Rises to 3x by level 99.
    private static final int[] FATIGUE_BASE_PER_HOUR = {400, 200, 100, 40, 12, 10, 8, 6, 5, 4};
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

    // Registry name of the node block (and its item), e.g. "oak_node", or of a fishing spot's
    // shoal item, e.g. "cod_shoal"
    public String blockName() {
        return getSerializedName() + (isSpot() ? "_shoal" : "_node");
    }

    // Lang key for the node's name
    public String translationKey() {
        return (isSpot() ? "item.skillcraft." : "block.skillcraft.") + blockName();
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    // Lang key for the short resource name used in heart names, e.g. "Oak", "Diamond", "Cod"
    public String heartKey() {
        return "heart.skillcraft." + getSerializedName();
    }

    private static final Map<Item, NodeType> BY_RESOURCE = new HashMap<>();

    // The node that produces this item, or null (e.g. vanilla fishing checks a fish's level)
    public static NodeType byResource(Item item) {
        if (BY_RESOURCE.isEmpty()) {
            for (NodeType type : values()) BY_RESOURCE.put(type.drop(), type);
        }
        return BY_RESOURCE.get(item);
    }
}
