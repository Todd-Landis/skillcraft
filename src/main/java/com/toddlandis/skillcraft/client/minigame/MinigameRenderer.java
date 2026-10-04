package com.toddlandis.skillcraft.client.minigame;

import com.toddlandis.skillcraft.altar.minigame.MinigameLogic;
import com.toddlandis.skillcraft.altar.minigame.ScoreTrack;
import com.toddlandis.skillcraft.altar.minigame.SetTheHookLogic;
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
            case SetTheHookLogic hook -> renderHook(hook, graphics, x, y, partialTick);
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

    // A float on a strip of water: it twitches at a nibble and is dragged under on a bite
    private static void renderHook(SetTheHookLogic logic, GuiGraphicsExtractor graphics, int x, int y, float partialTick) {
        int border = borderColor(logic.score(), 0xFF2C5A7A);
        int top = y + BAR_Y, bottom = top + BAR_H, surface = top + 6;
        graphics.fill(x - 1, top - 1, x + WIDTH + 1, bottom + 1, border);
        graphics.fill(x, top, x + WIDTH, bottom, 0xFF0E2A3E);
        graphics.fill(x, surface, x + WIDTH, bottom, 0xFF16405C);
        // Gentle moving wave crests
        float time = logic.ticks() + partialTick;
        for (int i = 0; i < WIDTH; i += 14) {
            int wx = x + (int) ((i + time * 0.6f) % WIDTH);
            graphics.fill(wx, surface, Math.min(x + WIDTH, wx + 6), surface + 1, 0xFF3A7CA5);
        }

        int cx = x + WIDTH / 2;
        int bob = (int) Math.round(Math.sin(time * 0.25) * 1.0);
        int sink = switch (logic.phase()) {
            case WAITING -> 0;
            case NIBBLE -> 2;
            case BITE -> 6;
        };
        int floatTop = surface - 5 + bob + sink;
        // Float: red cap over a white body, clipped at the bottom of the strip
        graphics.fill(cx - 2, floatTop, cx + 2, Math.min(bottom, floatTop + 3), 0xFFE03A2E);
        graphics.fill(cx - 2, Math.min(bottom, floatTop + 3), cx + 2, Math.min(bottom, floatTop + 8), 0xFFF2F2F2);
        // Line up out of the strip
        graphics.fill(cx, top - 6, cx + 1, floatTop, 0xC0E8E8E8);

        if (logic.phase() == SetTheHookLogic.Phase.BITE) {
            // Rings spread out from the float while the bite lasts
            int ring = 4 + (int) (logic.biteProgress() * 26);
            graphics.fill(cx - ring, surface - 1, cx - ring + 3, surface + 1, 0xFFBFE6FF);
            graphics.fill(cx + ring - 3, surface - 1, cx + ring, surface + 1, 0xFFBFE6FF);
            graphics.fill(cx - 1, top - 4, cx + 2, top - 3, 0xFFFFD34A);
        } else if (logic.phase() == SetTheHookLogic.Phase.NIBBLE) {
            graphics.fill(cx - 6, surface, cx - 4, surface + 1, 0xFF8FC4E6);
            graphics.fill(cx + 4, surface, cx + 6, surface + 1, 0xFF8FC4E6);
        }
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
