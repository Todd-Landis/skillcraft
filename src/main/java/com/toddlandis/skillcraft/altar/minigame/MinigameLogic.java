package com.toddlandis.skillcraft.altar.minigame;

import net.minecraft.network.chat.Component;

// The rules of one attunement minigame, with no drawing code, so the same logic runs on the
// client (to play) and on the server (to verify, see MinigameReplay).
//
// Logic must be deterministic: all randomness comes from the seed passed to the constructor,
// and time only advances through tick(). Drawing lives in client/minigame/MinigameRenderer.
//
// To add a minigame: implement this, add a MinigameKind, map it in Minigames.create(),
// and add a draw method in MinigameRenderer.
public interface MinigameLogic {
    enum State { RUNNING, SUCCESS, FAILURE }

    Component instructions();

    // Advance one game tick (20 per second)
    void tick();

    // Action button pressed / released (Space or left mouse)
    void press();

    default void release() {}

    ScoreTrack score();

    default State state() {
        return score().state();
    }

    // True once the player has done anything, so leaving early counts as a failure
    boolean started();
}
