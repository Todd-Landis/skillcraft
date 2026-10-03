package com.toddlandis.skillcraft.client.minigame;

import com.toddlandis.skillcraft.node.MinigameKind;

// Maps each MinigameKind to its implementation. tier is 0 (Tier I) to 6 (Tier VII); higher is harder.
public final class Minigames {
    private Minigames() {}

    public static Minigame create(MinigameKind kind, int tier) {
        return switch (kind) {
            case STRIKE_THE_GRAIN -> new StrikeTheGrainMinigame(tier);
            case TEMPER_THE_VEIN -> new TemperTheVeinMinigame(tier);
        };
    }
}
