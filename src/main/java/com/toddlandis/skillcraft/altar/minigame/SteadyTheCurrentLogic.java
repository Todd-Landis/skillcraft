package com.toddlandis.skillcraft.altar.minigame;

import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;

// Leycraft careful weave (docs/LEYCRAFT.md section 6): hold to make the ley current rise, let go
// to let it sink; it has momentum, so you feather it. A band drifts along the channel and a pulse
// comes round every PULSE_TICKS: current inside the band is a hit, outside a miss. Pulses only
// count after the first press, so closing it untouched is free.
// Higher tiers: narrower band that drifts faster.
public class SteadyTheCurrentLogic implements MinigameLogic {
    public static final int PULSE_TICKS = 25;
    private static final double LIFT = 0.0032;   // added to velocity per tick while held
    private static final double SINK = 0.0032;   // taken from velocity per tick while released
    private static final double MAX_SPEED = 0.035;

    private final RandomSource random;
    private final ScoreTrack score = new ScoreTrack(8, 3);
    private final double bandWidth;
    private final double driftSpeed;
    private double current;
    private double prevCurrent;
    private double velocity;
    private double bandCenter = 0.5;
    private double prevBandCenter = 0.5;
    private double bandTarget;
    private boolean holding;
    private boolean touched;
    private int pulseTimer = PULSE_TICKS;

    public SteadyTheCurrentLogic(int tier, long seed) {
        this.random = RandomSource.create(seed);
        this.bandWidth = Math.max(0.14, 0.28 - 0.03 * tier);
        this.driftSpeed = 0.006 + 0.002 * tier;
        pickTarget();
    }

    private void pickTarget() {
        double half = bandWidth / 2;
        bandTarget = half + 0.05 + random.nextDouble() * (1 - bandWidth - 0.1);
        bandTarget = Math.clamp(bandTarget, half, 1 - half);
    }

    @Override
    public Component instructions() {
        return Component.translatable("minigame.skillcraft.steady_the_current");
    }

    @Override
    public void tick() {
        score.tick();
        prevCurrent = current;
        prevBandCenter = bandCenter;
        if (state() != State.RUNNING) return;

        // The current: momentum, held up against the ends of the channel
        velocity = Math.clamp(velocity + (holding ? LIFT : -SINK), -MAX_SPEED, MAX_SPEED);
        current += velocity;
        if (current <= 0) { current = 0; velocity = 0; }
        if (current >= 1) { current = 1; velocity = 0; }

        // The band drifts toward a target, then picks a new one
        double delta = bandTarget - bandCenter;
        if (Math.abs(delta) <= driftSpeed) {
            bandCenter = bandTarget;
            pickTarget();
        } else {
            bandCenter += Math.signum(delta) * driftSpeed;
        }

        if (!touched) return;
        if (--pulseTimer <= 0) {
            pulseTimer = PULSE_TICKS;
            if (inBand()) score.hit(); else score.miss();
        }
    }

    public boolean inBand() {
        return Math.abs(current - bandCenter) <= bandWidth / 2;
    }

    @Override
    public void press() {
        if (state() != State.RUNNING) return;
        holding = true;
        touched = true;
    }

    @Override
    public void release() {
        holding = false;
    }

    @Override
    public ScoreTrack score() { return score; }

    @Override
    public boolean started() { return touched; }

    // For drawing (and the GameTest autopilot)
    public double current(float partialTick) { return prevCurrent + (current - prevCurrent) * partialTick; }
    public double bandCenter(float partialTick) { return prevBandCenter + (bandCenter - prevBandCenter) * partialTick; }
    public double bandWidth() { return bandWidth; }
    public double velocity() { return velocity; }
    public boolean holding() { return holding; }
    // 0 just after a pulse, 1 as the next one arrives
    public double pulseProgress(float partialTick) {
        if (!touched) return 0;
        return Math.clamp((PULSE_TICKS - pulseTimer + partialTick) / PULSE_TICKS, 0, 1);
    }
}
