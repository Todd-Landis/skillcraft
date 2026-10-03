package com.toddlandis.skillcraft.client.minigame;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

// One attunement minigame. AttunementScreen hosts it: it forwards the action button
// (Space or left mouse) and ticks it 20 times a second, and sends the result to the server
// once state() is no longer RUNNING. To add a minigame: implement this, add a MinigameKind,
// and map it in Minigames.create().
public interface Minigame {
    enum State { RUNNING, SUCCESS, FAILURE }

    // Width and height of the area the minigame draws in
    int WIDTH = 220;
    int HEIGHT = 44;

    Component instructions();

    void tick();

    void render(GuiGraphicsExtractor graphics, Font font, int x, int y, float partialTick);

    // Action button pressed / released
    void press();

    default void release() {}

    State state();

    // True once the player has done anything, so leaving early counts as a failure
    boolean started();
}
