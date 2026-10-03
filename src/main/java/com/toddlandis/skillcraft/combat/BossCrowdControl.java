package com.toddlandis.skillcraft.combat;

// Bosses implement this to receive Daze from stun abilities (spec 2.6): the boss's next attack
// comes a little later, at most once per window, and an attack that has already started winding
// up is never cancelled. Entities in the skillcraft:bosses entity tag also get softened slows and
// no ability knockback, whether or not they implement this.
public interface BossCrowdControl {
    // Returns true if the daze took effect (false while it's on its once-per-window lockout)
    boolean daze(int extraTicks);
}
