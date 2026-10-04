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

    // Leycraft (docs/LEYCRAFT.md; resonance blocks, essences and motes live in data maps)
    public static final ModConfigSpec.IntValue LEY_SCAN_RADIUS = BUILDER
            .comment("Blocks each way a Ley Altar scans for resonance (4 = a 9x9x9 cube).")
            .defineInRange("leyScanRadius", 4, 1, 8);

    public static final ModConfigSpec.DoubleValue LEY_POINTS_FOR_FULL = BUILDER
            .comment("Resonance points (from ley_resonance.json) that make an element 100%.")
            .defineInRange("leyPointsForFull", 40.0, 1.0, 10000.0);

    public static final ModConfigSpec.DoubleValue LEY_BIOME_BONUS = BUILDER
            .comment("Resonance added when the altar stands in one of an element's biomes (skillcraft:ley_resonance/<element> biome tags).")
            .defineInRange("leyBiomeBonus", 0.25, 0.0, 1.0);

    public static final ModConfigSpec.DoubleValue LEY_THUNDER_BONUS = BUILDER
            .comment("Storm resonance added during a thunderstorm when the altar is under open sky.")
            .defineInRange("leyThunderBonus", 0.5, 0.0, 1.0);

    public static final ModConfigSpec.DoubleValue LEY_RAIN_BONUS = BUILDER
            .comment("Storm resonance added while it rains (but doesn't thunder) when the altar is under open sky.")
            .defineInRange("leyRainBonus", 0.15, 0.0, 1.0);

    public static final ModConfigSpec.DoubleValue LEY_HEIGHT_BONUS = BUILDER
            .comment("Storm resonance added when the altar is under open sky at or above leyHeightY.")
            .defineInRange("leyHeightBonus", 0.25, 0.0, 1.0);

    public static final ModConfigSpec.IntValue LEY_HEIGHT_Y = BUILDER
            .comment("Height (Y) from which an open-sky altar gets the storm height bonus.")
            .defineInRange("leyHeightY", 120, -64, 320);

    public static final ModConfigSpec.DoubleValue LEY_CAREFUL_BEST = BUILDER
            .comment("Motes multiplier for a careful weave won with no misses.")
            .defineInRange("leyCarefulBest", 1.5, 1.0, 10.0);

    public static final ModConfigSpec.DoubleValue LEY_CAREFUL_PER_MISS = BUILDER
            .comment("How much each miss takes off a won careful weave's multiplier.")
            .defineInRange("leyCarefulPerMiss", 0.1, 0.0, 1.0);

    public static final ModConfigSpec.DoubleValue LEY_CAREFUL_FAILED = BUILDER
            .comment("Motes multiplier for a lost careful weave (a quick weave is 1.0).")
            .defineInRange("leyCarefulFailed", 0.8, 0.0, 1.0);

    public static final ModConfigSpec.DoubleValue LEY_CAREFUL_XP_BONUS = BUILDER
            .comment("Extra Leycraft XP for a won careful weave (0.25 = +25%).")
            .defineInRange("leyCarefulXpBonus", 0.25, 0.0, 10.0);

    public static final ModConfigSpec.DoubleValue LEY_LEVEL_BONUS = BUILDER
            .comment("Extra motes at Leycraft 99, rising from 0 at the mote's level (0.5 = +50%).")
            .defineInRange("leyLevelBonus", 0.5, 0.0, 10.0);

    public static final ModConfigSpec.IntValue LEY_TUNER_RADIUS = BUILDER
            .comment("Blocks a Ley Tuner searches for a strong source at Leycraft 0; it adds half the player's Leycraft level.")
            .defineInRange("leyTunerRadius", 32, 8, 128);

    public static final ModConfigSpec.DoubleValue LEY_TUNER_MIN_POINTS = BUILDER
            .comment("Resonance points (ley_resonance.json) a block needs to count as a strong source for the Ley Tuner.")
            .defineInRange("leyTunerMinPoints", 3.0, 0.0, 1000.0);

    public static final ModConfigSpec.IntValue LEY_TUNER_BIOME_RADIUS = BUILDER
            .comment("Blocks a Ley Tuner searches for a biome of its element when no source block is near.")
            .defineInRange("leyTunerBiomeRadius", 1600, 0, 6400);

    public static final ModConfigSpec.IntValue LEY_TUNER_COOLDOWN = BUILDER
            .comment("Ticks between rings of a Ley Tuner.")
            .defineInRange("leyTunerCooldown", 60, 0, 1200);

    // Beastbonding (docs/BEASTBONDING.md; per-creature numbers live in data_maps/entity_type/bondable.json)
    public static final ModConfigSpec.IntValue BOND_SLOTS_BASE = BUILDER
            .comment("Bond Codex slots at Beastbonding 0; one more per bondSlotsEveryLevels levels.")
            .defineInRange("bondSlotsBase", 4, 1, 200);

    public static final ModConfigSpec.IntValue BOND_SLOTS_EVERY = BUILDER
            .comment("Beastbonding levels per extra Bond Codex slot.")
            .defineInRange("bondSlotsEveryLevels", 5, 1, 99);

    public static final ModConfigSpec.IntValue BOND_SECOND_COMPANION_LEVEL = BUILDER
            .comment("Beastbonding level from which two companions can be out at once.")
            .defineInRange("bondSecondCompanionLevel", 50, 1, 100);

    public static final ModConfigSpec.DoubleValue BOND_SPEED_PER_LEVEL = BUILDER
            .comment("Summoned mount speed bonus per Beastbonding level (0.0025 = +25% at 99).")
            .defineInRange("bondSpeedPerLevel", 0.0025, 0.0, 0.05);

    public static final ModConfigSpec.DoubleValue BOND_HEALTH_PER_LEVEL = BUILDER
            .comment("Summoned creature max health bonus per Beastbonding level (0.005 = +50% at 99).")
            .defineInRange("bondHealthPerLevel", 0.005, 0.0, 0.1);

    public static final ModConfigSpec.DoubleValue BOND_FIRST_PAGE_MULTIPLIER = BUILDER
            .comment("Bond XP multiplier the first time a species is bonded (a new codex page).")
            .defineInRange("bondFirstPageMultiplier", 5.0, 1.0, 100.0);

    public static final ModConfigSpec.DoubleValue BOND_COMPANION_XP_PER_DAMAGE = BUILDER
            .comment("Beastbonding XP per point of damage a bonded companion deals to a monster.")
            .defineInRange("bondCompanionXpPerDamage", 0.5, 0.0, 100.0);

    public static final ModConfigSpec.DoubleValue BOND_FETCH_XP = BUILDER
            .comment("Beastbonding XP per item stack a bonded fox fetches.")
            .defineInRange("bondFetchXp", 2.0, 0.0, 100.0);

    public static final ModConfigSpec.IntValue BOND_SHRINE_RADIUS = BUILDER
            .comment("How close (blocks) to a Rekindling Shrine a player must be to rekindle.")
            .defineInRange("bondShrineRadius", 4, 1, 16);

    public static final ModConfigSpec.IntValue BOND_SUMMON_COOLDOWN = BUILDER
            .comment("Ticks between summons.")
            .defineInRange("bondSummonCooldown", 40, 0, 1200);

    public static final ModConfigSpec.IntValue CLOUDRAY_LURE_Y = BUILDER
            .comment("Lowest Y at which a Cloudray Lure works (under open sky).")
            .defineInRange("cloudrayLureY", 140, -64, 320);

    public static final ModConfigSpec.IntValue CLOUDRAY_WILD_TICKS = BUILDER
            .comment("Ticks a lured, unbonded Cloudray stays before it flies off.")
            .defineInRange("cloudrayWildTicks", 3600, 200, 72000);

    static final ModConfigSpec SPEC = BUILDER.build();
}
