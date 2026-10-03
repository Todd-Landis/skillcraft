package com.toddlandis.skillcraft.skill;

// The Old School RuneScape experience curve: XP needed roughly doubles every 7 levels,
// so level 92 is about halfway to 99.
public final class XpTable {
    public static final int MAX_LEVEL = 99;
    public static final long MAX_XP = 200_000_000L;

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

    public static int levelForXp(long xp) {
        for (int level = MAX_LEVEL; level > 1; level--) {
            if (xp >= XP_FOR_LEVEL[level]) return level;
        }
        return 1;
    }
}
