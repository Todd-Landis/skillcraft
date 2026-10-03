package com.toddlandis.skillcraft.client;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.toddlandis.skillcraft.network.XpGainPayload;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;

// Floating "+25 Woodcutting" numbers that drift up and fade out on the right side of the screen.
public final class XpDropOverlay {
    private static final long DURATION_MS = 1800;
    private static final int RISE_PIXELS = 40;
    private static final int COLOR = 0xFFFFFF;
    private static final int LEVEL_UP_COLOR = 0xFFD700;

    private record Drop(Component text, int color, long startMillis) {}

    private static final List<Drop> DROPS = new ArrayList<>();

    private XpDropOverlay() {}

    public static void add(XpGainPayload payload) {
        Component text = payload.newLevel() > 0
                ? Component.translatable("hud.skillcraft.level_up", payload.skill().displayName(), payload.newLevel())
                : Component.translatable("hud.skillcraft.xp_drop", payload.amount(), payload.skill().displayName());
        DROPS.add(new Drop(text, payload.newLevel() > 0 ? LEVEL_UP_COLOR : COLOR, System.currentTimeMillis()));
    }

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (DROPS.isEmpty()) return;
        Font font = Minecraft.getInstance().font;
        long now = System.currentTimeMillis();
        int baseY = graphics.guiHeight() / 3;

        Iterator<Drop> it = DROPS.iterator();
        while (it.hasNext()) {
            Drop drop = it.next();
            float progress = (now - drop.startMillis()) / (float) DURATION_MS;
            if (progress >= 1f) {
                it.remove();
                continue;
            }
            int alpha = (int) (255 * Math.min(1f, (1f - progress) * 2f)); // fade during the second half
            if (alpha < 8) continue;
            int x = graphics.guiWidth() - font.width(drop.text()) - 6;
            int y = baseY - (int) (progress * RISE_PIXELS);
            graphics.text(font, drop.text(), x, y, ARGB.color(alpha, drop.color()), true);
        }
    }
}
