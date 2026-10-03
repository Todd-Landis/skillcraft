package com.toddlandis.skillcraft.client.minigame;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;

// Mining: hold to build heat in the vein, release while the heat is inside the gold band.
// Heat speeds up the longer you hold; letting it max out cracks the vein (a miss).
// Higher tiers: heat climbs faster, band is narrower.
public class TemperTheVeinMinigame implements Minigame {
    private static final int BAR_Y = 14;
    private static final int BAR_H = 16;

    private final RandomSource random = RandomSource.create();
    private final ScoreTrack score = new ScoreTrack(4, 2);
    private final double acceleration;
    private final double bandWidth;
    private double heat;
    private double prevHeat;
    private double velocity;
    private boolean holding;
    private boolean touched;
    private double bandStart;

    public TemperTheVeinMinigame(int tier) {
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
    public void render(GuiGraphicsExtractor graphics, Font font, int x, int y, float partialTick) {
        int border = score.borderColor(0xFF6E6E6E);
        graphics.fill(x - 1, y + BAR_Y - 1, x + WIDTH + 1, y + BAR_Y + BAR_H + 1, border);
        graphics.fill(x, y + BAR_Y, x + WIDTH, y + BAR_Y + BAR_H, 0xFF2A2A2E);

        double h = prevHeat + (heat - prevHeat) * partialTick;
        int filled = (int) (h * WIDTH);
        // Heat glows from dull red to bright yellow as it rises
        int red = 0xC0 + (int) (0x3F * h);
        int green = (int) (0xD0 * h);
        graphics.fill(x, y + BAR_Y + 2, x + filled, y + BAR_Y + BAR_H - 2, 0xFF000000 | red << 16 | green << 8 | 0x20);

        int bx = x + (int) (bandStart * WIDTH);
        int bw = (int) (bandWidth * WIDTH);
        graphics.fill(bx, y + BAR_Y - 2, bx + bw, y + BAR_Y, 0xFFFFD700);
        graphics.fill(bx, y + BAR_Y + BAR_H, bx + bw, y + BAR_Y + BAR_H + 2, 0xFFFFD700);

        score.render(graphics, x, y, WIDTH);
    }

    @Override
    public State state() {
        return score.state();
    }

    @Override
    public boolean started() {
        return touched;
    }
}
