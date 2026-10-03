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

    public static final ModConfigSpec.BooleanValue ABILITIES_AFFECT_PLAYERS = BUILDER
            .comment("Whether combat abilities can damage and crowd-control other players (PvP). When on, their damage\n"
                    + "and effects on players are halved, and roots and stuns last at most 1 second.")
            .define("abilitiesAffectPlayers", false);

    // Ability system constants (per-ability numbers live in data/*/skillcraft/ability/*.json)
    public static final ModConfigSpec.IntValue GLOBAL_COOLDOWN = BUILDER
            .comment("Ticks after using an ability before another (non off-GCD) ability can be used. 20 = 1 s.")
            .defineInRange("abilityGlobalCooldown", 20, 0, 200);

    public static final ModConfigSpec.IntValue COMBAT_WINDOW = BUILDER
            .comment("Ticks after dealing or taking entity damage during which a player counts as in combat (loadout swaps are blocked).")
            .defineInRange("combatWindow", 100, 0, 1200);

    public static final ModConfigSpec.DoubleValue MOMENTUM_PER_MELEE_HIT = BUILDER
            .comment("Momentum gained per melee hit that lands, times the swing's attack strength squared (weak spam hits give little).")
            .defineInRange("momentumPerMeleeHit", 6.0, 0.0, 100.0);

    public static final ModConfigSpec.DoubleValue MOMENTUM_PER_PROJECTILE_HIT = BUILDER
            .comment("Momentum gained per projectile hit that lands.")
            .defineInRange("momentumPerProjectileHit", 5.0, 0.0, 100.0);

    public static final ModConfigSpec.DoubleValue MOMENTUM_PER_HIT_TAKEN = BUILDER
            .comment("Momentum gained when an entity damages you (at most twice a second).")
            .defineInRange("momentumPerHitTaken", 2.0, 0.0, 100.0);

    public static final ModConfigSpec.IntValue MOMENTUM_DECAY_DELAY = BUILDER
            .comment("Ticks out of combat before Momentum starts draining.")
            .defineInRange("momentumDecayDelay", 200, 0, 12000);

    public static final ModConfigSpec.DoubleValue MOMENTUM_DECAY_PER_SECOND = BUILDER
            .comment("Momentum drained per second once the decay delay has passed.")
            .defineInRange("momentumDecayPerSecond", 5.0, 0.0, 100.0);

    static final ModConfigSpec SPEC = BUILDER.build();
}
