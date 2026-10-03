package com.toddlandis.skillcraft.client.minigame;

import com.toddlandis.skillcraft.altar.minigame.MinigameLogic;
import com.toddlandis.skillcraft.altar.minigame.ScoreTrack;
import com.toddlandis.skillcraft.altar.minigame.StrikeTheGrainLogic;
import com.toddlandis.skillcraft.altar.minigame.TemperTheVeinLogic;

import net.minecraft.client.gui.GuiGraphicsExtractor;

// Draws each minigame. The rules live in altar/minigame (shared with the server); this is
// purely visual. Add a case here when adding a new minigame.
public final class MinigameRenderer {
    public static final int WIDTH = 220;
    public static final int HEIGHT = 44;
    private static final int BAR_Y = 14;
    private static final int BAR_H = 16;
    private static final int PIP = 6;
    private static final int GAP = 3;

    private MinigameRenderer() {}

    public static void render(MinigameLogic logic, GuiGraphicsExtractor graphics, int x, int y, float partialTick) {
        switch (logic) {
            case StrikeTheGrainLogic grain -> renderGrain(grain, graphics, x, y, partialTick);
            case TemperTheVeinLogic vein -> renderVein(vein, graphics, x, y, partialTick);
            default -> {}
        }
        renderScore(logic.score(), graphics, x, y);
    }

    private static void renderGrain(StrikeTheGrainLogic logic, GuiGraphicsExtractor graphics, int x, int y, float partialTick) {
        int border = borderColor(logic.score(), 0xFF8B5A2B);
        graphics.fill(x - 1, y + BAR_Y - 1, x + WIDTH + 1, y + BAR_Y + BAR_H + 1, border);
        graphics.fill(x, y + BAR_Y, x + WIDTH, y + BAR_Y + BAR_H, 0xFF3B2414);
        // Wood grain stripes
        for (int i = 0; i < WIDTH; i += 11) graphics.fill(x + i, y + BAR_Y + 3, x + i + 6, y + BAR_Y + 4, 0xFF4A2F1A);

        int zx = x + (int) (logic.zoneStart() * WIDTH);
        graphics.fill(zx, y + BAR_Y, zx + (int) (logic.zoneWidth() * WIDTH), y + BAR_Y + BAR_H, 0xC040C040);

        int mx = x + (int) (logic.marker(partialTick) * (WIDTH - 2));
        graphics.fill(mx, y + BAR_Y - 3, mx + 2, y + BAR_Y + BAR_H + 3, 0xFFFFFFFF);
    }

    private static void renderVein(TemperTheVeinLogic logic, GuiGraphicsExtractor graphics, int x, int y, float partialTick) {
        int border = borderColor(logic.score(), 0xFF6E6E6E);
        graphics.fill(x - 1, y + BAR_Y - 1, x + WIDTH + 1, y + BAR_Y + BAR_H + 1, border);
        graphics.fill(x, y + BAR_Y, x + WIDTH, y + BAR_Y + BAR_H, 0xFF2A2A2E);

        double h = logic.heat(partialTick);
        int filled = (int) (h * WIDTH);
        // Heat glows from dull red to bright yellow as it rises
        int red = 0xC0 + (int) (0x3F * h);
        int green = (int) (0xD0 * h);
        graphics.fill(x, y + BAR_Y + 2, x + filled, y + BAR_Y + BAR_H - 2, 0xFF000000 | red << 16 | green << 8 | 0x20);

        int bx = x + (int) (logic.bandStart() * WIDTH);
        int bw = (int) (logic.bandWidth() * WIDTH);
        graphics.fill(bx, y + BAR_Y - 2, bx + bw, y + BAR_Y, 0xFFFFD700);
        graphics.fill(bx, y + BAR_Y + BAR_H, bx + bw, y + BAR_Y + BAR_H + 2, 0xFFFFD700);
    }

    // Border flashes green after a hit, red after a miss
    private static int borderColor(ScoreTrack score, int defaultColor) {
        if (!score.flashing()) return defaultColor;
        return score.lastWasHit() ? 0xFF55FF55 : 0xFFFF5555;
    }

    // Hit pips on the left, miss allowance on the right
    private static void renderScore(ScoreTrack score, GuiGraphicsExtractor graphics, int x, int y) {
        for (int i = 0; i < score.hitsNeeded(); i++) {
            int px = x + i * (PIP + GAP);
            graphics.fill(px, y, px + PIP, y + PIP, i < score.hits() ? 0xFF55FF55 : 0xFF333333);
        }
        for (int i = 0; i <= score.missesAllowed(); i++) {
            int px = x + WIDTH - (i + 1) * (PIP + GAP) + GAP;
            graphics.fill(px, y, px + PIP, y + PIP, i < score.misses() ? 0xFFFF5555 : 0xFF553333);
        }
    }
}
