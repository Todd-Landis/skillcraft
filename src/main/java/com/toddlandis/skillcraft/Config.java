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

    // Raised from 6/5/2 after the first playtest: Momentum felt too slow to build
    public static final ModConfigSpec.DoubleValue MOMENTUM_PER_MELEE_HIT = BUILDER
            .comment("Momentum gained per melee hit that lands, times the swing's attack strength squared (weak spam hits give little).")
            .defineInRange("momentumPerMeleeHit", 10.0, 0.0, 100.0);

    public static final ModConfigSpec.DoubleValue MOMENTUM_PER_PROJECTILE_HIT = BUILDER
            .comment("Momentum gained per projectile hit that lands.")
            .defineInRange("momentumPerProjectileHit", 9.0, 0.0, 100.0);

    public static final ModConfigSpec.DoubleValue MOMENTUM_PER_HIT_TAKEN = BUILDER
            .comment("Momentum gained when an entity damages you (at most twice a second).")
            .defineInRange("momentumPerHitTaken", 4.0, 0.0, 100.0);

    public static final ModConfigSpec.IntValue MOMENTUM_DECAY_DELAY = BUILDER
            .comment("Ticks out of combat before Momentum starts draining.")
            .defineInRange("momentumDecayDelay", 200, 0, 12000);

    public static final ModConfigSpec.DoubleValue MOMENTUM_DECAY_PER_SECOND = BUILDER
            .comment("Momentum drained per second once the decay delay has passed.")
            .defineInRange("momentumDecayPerSecond", 5.0, 0.0, 100.0);

    public static final ModConfigSpec.BooleanValue CONSUME_EMBER_SIGIL = BUILDER
            .comment("Whether using an Ember Sigil at the Summoning Brazier uses it up. Off: the sigil is a reusable key.")
            .define("consumeEmberSigil", false);

    // Prayer (docs/AGILITY_MAGIC_PRAYER.md section 3; per-blessing numbers live in the ability JSON)
    public static final ModConfigSpec.IntValue FAITH_BASE = BUILDER
            .comment("Maximum Faith = this + Prayer level.")
            .defineInRange("faithBase", 9, 0, 1000);

    public static final ModConfigSpec.IntValue FAITH_REGEN_TICKS = BUILDER
            .comment("Ticks per point of Faith regained while no blessing is on (0 = no natural regeneration).")
            .defineInRange("faithRegenTicks", 600, 0, 72000);

    public static final ModConfigSpec.DoubleValue SHRINE_XP_MULTIPLIER = BUILDER
            .comment("Prayer XP multiplier for offering at a Votive Shrine instead of burying.")
            .defineInRange("shrineXpMultiplier", 3.0, 1.0, 100.0);

    // Agility (section 4)
    public static final ModConfigSpec.DoubleValue SAFE_FALL_PER_LEVEL = BUILDER
            .comment("Blocks of fall that stop hurting per Agility level (0.1 = one block per 10 levels).")
            .defineInRange("agilitySafeFallPerLevel", 0.1, 0.0, 1.0);

    public static final ModConfigSpec.DoubleValue SPRINT_SPEED_PER_LEVEL = BUILDER
            .comment("Sprint speed bonus per Agility level above 1 (0.0005 = +0.05% per level).")
            .defineInRange("agilitySprintSpeedPerLevel", 0.0005, 0.0, 0.01);

    public static final ModConfigSpec.IntValue PARKOUR_NOVELTY_TICKS = BUILDER
            .comment("Ticks before the same spot (a 4x4x4 cell) pays parkour XP again.")
            .defineInRange("parkourNoveltyTicks", 1200, 0, 72000);

    static final ModConfigSpec SPEC = BUILDER.build();
}
