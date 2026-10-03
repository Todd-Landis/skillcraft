package com.toddlandis.skillcraft.node;

// Which attunement minigame a node uses at the Attunement Altar. The client maps each kind
// to an implementation in client/minigame/Minigames.java, so minigames can be swapped or added
// without touching the altar or networking code.
public enum MinigameKind {
    STRIKE_THE_GRAIN, // tap when a sweeping marker is inside the zone (woodcutting)
    TEMPER_THE_VEIN   // hold to build heat, release inside the target band (mining)
}
