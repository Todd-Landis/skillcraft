package com.toddlandis.skillcraft.ability;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.toddlandis.skillcraft.ability.behavior.NextShot;
import com.toddlandis.skillcraft.registry.ModAttachments;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

// Server-side, never saved: cooldowns, the global cooldown, a windup in progress and the
// short-lived states some abilities leave behind (next-shot bonuses, parry windows, heals).
// It's a transient attachment, so it resets on logout and death (spec 2.3: harmless).
public final class AbilityRuntime {
    public final Map<Identifier, Long> readyAt = new HashMap<>();
    public long gcdUntil;

    // Windup: the ability fires at windupEnd unless the player moves away from windupPos
    public Identifier windupId;
    public long windupEnd;
    public Vec3 windupPos;

    // Bonus for the next projectile the player fires (Quick Draw, Pinning Shot, Tumble...)
    public NextShot.Shot nextShot;
    public long nextShotUntil;

    // Riposte: hits are negated until parryUntil; then the next melee hit before counterUntil is boosted
    public long parryUntil;
    public long counterUntil;
    public int counterWindow;
    public float counterBonus;
    public int counterMomentum;

    // Heal over time (Second Wind)
    public float healPerSecond;
    public long healUntil;

    // Toggle abilities that are on
    public final Set<Identifier> toggles = new LinkedHashSet<>();

    // Attack strength of the swing that is about to land (set in AttackEntityEvent), -1 if unknown
    public float pendingStrength = -1;
    // "Long ago", so the first hit counts even in a brand-new world (game time starts at 0)
    public long lastHitTakenMomentum = Long.MIN_VALUE / 2;

    // Set by a behaviour that fails for a specific reason (e.g. no ammo), shown instead of "no target"
    public String failKey;

    // Throttle for "can't use" feedback
    public long lastDenyTick = Long.MIN_VALUE / 2;
    public String lastDenyKey = "";

    public static AbilityRuntime of(Player player) {
        return player.getData(ModAttachments.ABILITY_RUNTIME);
    }

    public long readyAt(Identifier id) {
        return readyAt.getOrDefault(id, 0L);
    }
}
