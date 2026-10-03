package com.toddlandis.skillcraft.combat;

// Implemented by entities whose weaknesses change at runtime (e.g. a boss switching stances).
// Takes priority over the entity type's combat_profiles entry.
public interface StyleVulnerable {
    double damageModifier(DamageStyle style);
}
