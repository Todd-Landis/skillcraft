package com.toddlandis.skillcraft.altar.minigame;

import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;

// Mining: hold to build heat in the vein, release while the heat is inside the gold band.
// Heat speeds up the longer you hold; letting it max out cracks the vein (a miss).
// Higher tiers: heat climbs faster, band is narrower.
public class TemperTheVeinLogic implements MinigameLogic {
    private final RandomSource random;
    private final ScoreTrack score = new ScoreTrack(4, 2);
    private final double acceleration;
    private final double bandWidth;
    private double heat;
    private double prevHeat;
    private double velocity;
    private boolean holding;
    private boolean touched;
    private double bandStart;

    public TemperTheVeinLogic(int tier, long seed) {
        this.random = RandomSource.create(seed);
        this.acceleration = 0.0009 + 0.00025 * tier;
        this.bandWidth = Math.max(0.07, 0.20 - 0.02 * tier);
        moveBand();
    }

    private void moveBand() {
        bandStart = 0.35 + random.nextDouble() * (0.95 - 0.35 - bandWidth);
    }

    @Override
    public Component instructions() {
        return Component.translatable("minigame.skillcraft.temper_the_vein");
    }

    @Override
    public void tick() {
        score.tick();
        prevHeat = heat;
        if (holding && state() == State.RUNNING) {
            velocity += acceleration;
            heat += velocity;
            if (heat >= 1) { // overheated
                score.miss();
                reset();
            }
        } else {
            heat = Math.max(0, heat - 0.03); // cools off when not held
        }
    }

    private void reset() {
        holding = false;
        heat = 0;
        prevHeat = 0;
        velocity = 0;
    }

    @Override
    public void press() {
        if (state() != State.RUNNING) return;
        holding = true;
        touched = true;
    }

    @Override
    public void release() {
        if (!holding || state() != State.RUNNING) return;
        if (heat >= bandStart && heat <= bandStart + bandWidth) {
            score.hit();
            moveBand();
        } else {
            score.miss();
        }
        reset();
    }

    @Override
    public ScoreTrack score() { return score; }

    @Override
    public boolean started() { return touched; }

    // For drawing
    public double heat(float partialTick) { return prevHeat + (heat - prevHeat) * partialTick; }
    public double bandStart() { return bandStart; }
    public double bandWidth() { return bandWidth; }
}
