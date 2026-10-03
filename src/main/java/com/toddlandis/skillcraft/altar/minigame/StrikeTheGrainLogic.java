package com.toddlandis.skillcraft.altar.minigame;

import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;

// Woodcutting: a marker sweeps back and forth along the grain. Strike while it is inside the
// green zone. Higher tiers: faster marker, narrower zone.
public class StrikeTheGrainLogic implements MinigameLogic {
    private final RandomSource random;
    private final ScoreTrack score = new ScoreTrack(5, 2);
    private final double speed;     // bar lengths per tick
    private final double zoneWidth; // fraction of the bar
    private double marker;
    private double prevMarker;
    private int direction = 1;
    private double zoneStart;

    public StrikeTheGrainLogic(int tier, long seed) {
        this.random = RandomSource.create(seed);
        this.speed = (0.7 + 0.18 * tier) / 20.0;
        this.zoneWidth = Math.max(0.08, 0.22 - 0.022 * tier);
        moveZone();
    }

    private void moveZone() {
        zoneStart = random.nextDouble() * (1 - zoneWidth);
    }

    @Override
    public Component instructions() {
        return Component.translatable("minigame.skillcraft.strike_the_grain");
    }

    @Override
    public void tick() {
        score.tick();
        prevMarker = marker;
        marker += speed * direction;
        if (marker >= 1) { marker = 1; direction = -1; }
        if (marker <= 0) { marker = 0; direction = 1; }
    }

    @Override
    public void press() {
        if (state() != State.RUNNING) return;
        if (marker >= zoneStart && marker <= zoneStart + zoneWidth) {
            score.hit();
            moveZone();
        } else {
            score.miss();
        }
    }

    @Override
    public ScoreTrack score() { return score; }

    @Override
    public boolean started() { return score.any(); }

    // For drawing
    public double marker(float partialTick) { return prevMarker + (marker - prevMarker) * partialTick; }
    public double zoneStart() { return zoneStart; }
    public double zoneWidth() { return zoneWidth; }
}
