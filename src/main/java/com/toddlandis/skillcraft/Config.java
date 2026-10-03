package com.toddlandis.skillcraft;

import net.neoforged.neoforge.common.ModConfigSpec;

// Server-wide tuning values, saved to config/skillcraft-common.toml.
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.DoubleValue XP_MULTIPLIER = BUILDER
            .comment("Multiplier applied to all skill XP gains. 1.0 = OSRS-like pacing.")
            .defineInRange("xpMultiplier", 1.0, 0.1, 100.0);

    public static final ModConfigSpec.IntValue BANK_SLOTS = BUILDER
            .comment("Number of slots in each player's bank. Stackable items use one slot per item type.")
            .defineInRange("bankSlots", 800, 1, 100000);

    static final ModConfigSpec SPEC = BUILDER.build();
}
