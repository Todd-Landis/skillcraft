package com.toddlandis.skillcraft.client.minigame;

import net.minecraft.client.gui.GuiGraphicsExtractor;

// Shared hit/miss counting and the row of progress pips, so each minigame only handles its own mechanic.
public class ScoreTrack {
    private static final int PIP = 6;
    private static final int GAP = 3;

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

    public Minigame.State state() {
        if (hits >= hitsNeeded) return Minigame.State.SUCCESS;
        if (misses > missesAllowed) return Minigame.State.FAILURE;
        return Minigame.State.RUNNING;
    }

    public boolean any() {
        return hits > 0 || misses > 0;
    }

    // Border color while flashing: green after a hit, red after a miss, otherwise the default
    public int borderColor(int defaultColor) {
        if (flashTicks == 0) return defaultColor;
        return lastWasHit ? 0xFF55FF55 : 0xFFFF5555;
    }

    // Hit pips on the left, remaining miss allowance on the right
    public void render(GuiGraphicsExtractor graphics, int x, int y, int width) {
        for (int i = 0; i < hitsNeeded; i++) {
            int px = x + i * (PIP + GAP);
            graphics.fill(px, y, px + PIP, y + PIP, i < hits ? 0xFF55FF55 : 0xFF333333);
        }
        for (int i = 0; i <= missesAllowed; i++) {
            int px = x + width - (i + 1) * (PIP + GAP) + GAP;
            graphics.fill(px, y, px + PIP, y + PIP, i < misses ? 0xFFFF5555 : 0xFF553333);
        }
    }
}
