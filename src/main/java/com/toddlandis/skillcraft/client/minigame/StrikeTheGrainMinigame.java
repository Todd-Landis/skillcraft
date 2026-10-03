package com.toddlandis.skillcraft.client.minigame;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;

// Woodcutting: a marker sweeps back and forth along the grain. Strike while it is inside the
// green zone. Higher tiers: faster marker, narrower zone.
public class StrikeTheGrainMinigame implements Minigame {
    private static final int BAR_Y = 14;
    private static final int BAR_H = 16;

    private final RandomSource random = RandomSource.create();
    private final ScoreTrack score = new ScoreTrack(5, 2);
    private final double speed;     // bar lengths per tick
    private final double zoneWidth; // fraction of the bar
    private double marker;
    private double prevMarker;
    private int direction = 1;
    private double zoneStart;

    public StrikeTheGrainMinigame(int tier) {
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
    public void render(GuiGraphicsExtractor graphics, Font font, int x, int y, float partialTick) {
        int border = score.borderColor(0xFF8B5A2B);
        graphics.fill(x - 1, y + BAR_Y - 1, x + WIDTH + 1, y + BAR_Y + BAR_H + 1, border);
        graphics.fill(x, y + BAR_Y, x + WIDTH, y + BAR_Y + BAR_H, 0xFF3B2414);
        // Wood grain stripes
        for (int i = 0; i < WIDTH; i += 11) graphics.fill(x + i, y + BAR_Y + 3, x + i + 6, y + BAR_Y + 4, 0xFF4A2F1A);

        int zx = x + (int) (zoneStart * WIDTH);
        graphics.fill(zx, y + BAR_Y, zx + (int) (zoneWidth * WIDTH), y + BAR_Y + BAR_H, 0xC040C040);

        double m = prevMarker + (marker - prevMarker) * partialTick;
        int mx = x + (int) (m * (WIDTH - 2));
        graphics.fill(mx, y + BAR_Y - 3, mx + 2, y + BAR_Y + BAR_H + 3, 0xFFFFFFFF);

        score.render(graphics, x, y, WIDTH);
    }

    @Override
    public State state() {
        return score.state();
    }

    @Override
    public boolean started() {
        return score.any();
    }
}
