package com.toddlandis.skillcraft.altar.minigame;

// Shared hit/miss counting for minigames, so each minigame only handles its own mechanic.
public class ScoreTrack {
    private final int hitsNeeded;
    private final int missesAllowed;
    private int hits;
    private int misses;
    private int flashTicks;
    private boolean lastWasHit;

    public ScoreTrack(int hitsNeeded, int missesAllowed) {
        this.hitsNeeded = hitsNeeded;
        this.missesAllowed = missesAllowed;
    }

    public void hit() { hits++; flash(true); }
    public void miss() { misses++; flash(false); }

    private void flash(boolean hit) {
        lastWasHit = hit;
        flashTicks = 6;
    }

    public void tick() {
        if (flashTicks > 0) flashTicks--;
    }

    public MinigameLogic.State state() {
        if (hits >= hitsNeeded) return MinigameLogic.State.SUCCESS;
        if (misses > missesAllowed) return MinigameLogic.State.FAILURE;
        return MinigameLogic.State.RUNNING;
    }

    public boolean any() { return hits > 0 || misses > 0; }
    public int hits() { return hits; }
    public int misses() { return misses; }
    public int hitsNeeded() { return hitsNeeded; }
    public int missesAllowed() { return missesAllowed; }
    public boolean flashing() { return flashTicks > 0; }
    public boolean lastWasHit() { return lastWasHit; }
}
