package com.toddlandis.skillcraft.skill;

import java.text.DecimalFormat;

// The Old School RuneScape experience curve: XP needed roughly doubles every 7 levels,
// so level 92 is about halfway to 99.
public final class XpTable {
    public static final int MAX_LEVEL = 99;
    public static final long MAX_XP = 200_000_000L;

    // Content tiers are 10-level bands: I = 1, II = 10, III = 20 ... X = 90.
    // Vanilla content lives in tiers I-V (up to ~50); VI-X are reserved for custom content.
    public static final int LEVELS_PER_TIER = 10;
    private static final String[] TIER_NUMERALS = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};

    // XP_FOR_LEVEL[n] = total XP required to reach level n (index 0 unused)
    private static final long[] XP_FOR_LEVEL = new long[MAX_LEVEL + 1];

    static {
        double points = 0;
        XP_FOR_LEVEL[1] = 0;
        for (int level = 1; level < MAX_LEVEL; level++) {
            points += Math.floor(level + 300.0 * Math.pow(2.0, level / 7.0));
            XP_FOR_LEVEL[level + 1] = (long) Math.floor(points / 4.0);
        }
    }

    private XpTable() {}

    public static long xpForLevel(int level) {
        if (level <= 1) return 0;
        return XP_FOR_LEVEL[Math.min(level, MAX_LEVEL)];
    }

    public static int levelForXp(double xp) {
        for (int level = MAX_LEVEL; level > 1; level--) {
            if (xp >= XP_FOR_LEVEL[level]) return level;
        }
        return 1;
    }

    // 0-based tier index for a required level: 1-9 -> 0, 10-19 -> 1, ...
    public static int tierIndex(int requiredLevel) {
        return Math.min(requiredLevel / LEVELS_PER_TIER, TIER_NUMERALS.length - 1);
    }

    public static String tierName(int requiredLevel) {
        return TIER_NUMERALS[tierIndex(requiredLevel)];
    }

    // "1,234.5" - XP with up to 2 decimals, used everywhere XP is shown
    public static String format(double xp) {
        return new DecimalFormat("#,##0.##").format(xp);
    }
}
