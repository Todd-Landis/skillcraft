package com.toddlandis.skillcraft.altar.minigame;

// Which attunement minigame a node uses at the Attunement Altar. The client maps each kind
// to an implementation in Minigames.create(), so minigames can be swapped or added
// without touching the altar or networking code.
public enum MinigameKind {
    STRIKE_THE_GRAIN, // tap when a sweeping marker is inside the zone (woodcutting)
    TEMPER_THE_VEIN,  // hold to build heat, release inside the target band (mining)
    SET_THE_HOOK,     // strike when the float is pulled under, not at nibbles (fishing)
    STEADY_THE_CURRENT // keep the ley current inside a drifting band (Leycraft careful weave)
}
