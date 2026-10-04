package com.toddlandis.skillcraft.boss;

import java.util.Map;

import com.toddlandis.skillcraft.combat.DamageStyle;

// Fallback tuning used if the boss_tuning data map entry is missing (e.g. a broken datapack).
// Keep in sync with data/skillcraft/data_maps/entity_type/boss_tuning.json.
public final class BossDefaults {
    public static final BossTuning SLAGHEART = new BossTuning(500, 0.6,
            Map.of("hammerfall", 14.0, "tong_sweep", 10.0, "slag_glob", 6.0, "ember_rain", 8.0, "forge_quake", 8.0, "heat_pulse", 4.0),
            Map.of("hammerfall", 30, "tong_sweep", 18, "slag_spit", 20, "ember_rain", 25, "forge_quake", 20),
            Map.of("1", 60, "2", 50, "3", 40), 1.5, 0.5, 360,
            Map.of("banked", Map.of(DamageStyle.STAB, 0.8, DamageStyle.SLASH, 0.6, DamageStyle.CRUSH, 1.25,
                            DamageStyle.LIGHT, 0.6, DamageStyle.MEDIUM, 0.85, DamageStyle.HEAVY, 1.2),
                    "vented", Map.of(DamageStyle.STAB, 1.5, DamageStyle.SLASH, 1.2, DamageStyle.CRUSH, 1.0,
                            DamageStyle.LIGHT, 1.4, DamageStyle.MEDIUM, 1.2, DamageStyle.HEAVY, 1.0)));

    private BossDefaults() {}
}
