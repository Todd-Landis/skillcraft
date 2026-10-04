package com.toddlandis.skillcraft.altar.minigame;

import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;

// Fishing: watch the float. Fish nibble (a small twitch: don't strike) and then bite (the float
// is pulled under: strike before it pops back up). Striking a nibble or still water is a miss,
// and so is letting a bite get away.
// Higher tiers: shorter bites, more nibbles before each bite.
public class SetTheHookLogic implements MinigameLogic {
    public enum Phase { WAITING, NIBBLE, BITE }

    private static final int NIBBLE_TICKS = 6;

    private final RandomSource random;
    private final ScoreTrack score = new ScoreTrack(5, 2);
    private final int biteTicks;
    private final int maxNibbles;
    private Phase phase = Phase.WAITING;
    private int phaseTicks;     // ticks left in the current phase
    private int nibblesLeft;    // nibbles before the next bite
    private int ticks;
    private boolean touched;

    public SetTheHookLogic(int tier, long seed) {
        this.random = RandomSource.create(seed);
        this.biteTicks = Math.max(7, 16 - 2 * tier);
        this.maxNibbles = Math.min(4, 1 + tier / 2);
        nextFish();
    }

    // Quiet water, then some nibbles, then a bite
    private void nextFish() {
        phase = Phase.WAITING;
        phaseTicks = 15 + random.nextInt(30);
        nibblesLeft = random.nextInt(maxNibbles + 1);
    }

    @Override
    public Component instructions() {
        return Component.translatable("minigame.skillcraft.set_the_hook");
    }

    @Override
    public void tick() {
        score.tick();
        ticks++;
        if (state() != State.RUNNING) return;
        if (--phaseTicks > 0) return;
        switch (phase) {
            case WAITING -> {
                if (nibblesLeft > 0) {
                    nibblesLeft--;
                    phase = Phase.NIBBLE;
                    phaseTicks = NIBBLE_TICKS;
                } else {
                    phase = Phase.BITE;
                    phaseTicks = biteTicks;
                }
            }
            case NIBBLE -> {
                phase = Phase.WAITING;
                phaseTicks = 8 + random.nextInt(16);
            }
            case BITE -> {
                if (touched) score.miss(); // too slow, the fish got away (free until the first strike)
                nextFish();
            }
        }
    }

    @Override
    public void press() {
        if (state() != State.RUNNING) return;
        touched = true;
        if (phase == Phase.BITE) {
            score.hit();
        } else {
            score.miss(); // struck at a nibble or at nothing
        }
        nextFish();
    }

    @Override
    public ScoreTrack score() { return score; }

    @Override
    public boolean started() { return touched; } // letting bites go by without ever striking is not playing

    // For drawing
    public Phase phase() { return phase; }
    public int ticks() { return ticks; }
    public float biteProgress() { return phase == Phase.BITE ? 1f - phaseTicks / (float) biteTicks : 0f; }
}
