package com.toddlandis.skillcraft.client;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.platform.InputConstants;
import com.toddlandis.skillcraft.altar.AttunementAltarBlock;
import com.toddlandis.skillcraft.altar.minigame.MinigameLogic;
import com.toddlandis.skillcraft.altar.minigame.Minigames;
import com.toddlandis.skillcraft.client.minigame.MinigameRenderer;
import com.toddlandis.skillcraft.network.AttunementResultPayload;
import com.toddlandis.skillcraft.network.OpenAttunementPayload;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

// Hosts whichever minigame the node uses. Records every button press/release with the tick it
// happened on and sends that list to the server, which replays the minigame from the same seed
// to decide the result (see AltarAttunement). Shows the local outcome briefly, then closes.
public class AttunementScreen extends Screen {
    private static final int RESULT_DISPLAY_TICKS = 30;
    private static final int PANEL_W = MinigameRenderer.WIDTH + 24;
    private static final int PANEL_H = MinigameRenderer.HEIGHT + 52;

    private final BlockPos altarPos;
    private final MinigameLogic minigame;
    private final List<Minigames.Input> inputs = new ArrayList<>();
    private int ticks;
    private boolean sent;
    private int closeTimer;
    private boolean buttonDown;

    public AttunementScreen(OpenAttunementPayload payload) {
        super(Component.translatable("screen.skillcraft.attunement", AttunementAltarBlock.nodeName(payload.node())));
        this.altarPos = payload.pos();
        this.minigame = Minigames.create(payload.node().minigame(), payload.node().tierIndex(), payload.seed());
    }

    @Override
    public void tick() {
        if (!sent) {
            minigame.tick();
            ticks++;
            checkFinished();
        } else if (++closeTimer >= RESULT_DISPLAY_TICKS) {
            super.onClose();
        }
    }

    private void input(boolean press) {
        if (sent || inputs.size() >= Minigames.MAX_INPUTS) return;
        inputs.add(new Minigames.Input(ticks, press));
        if (press) minigame.press(); else minigame.release();
        checkFinished();
    }

    private void checkFinished() {
        if (minigame.state() != MinigameLogic.State.RUNNING) send();
    }

    private void send() {
        sent = true;
        ClientPacketDistributor.sendToServer(new AttunementResultPayload(altarPos, List.copyOf(inputs)));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        int left = (width - PANEL_W) / 2;
        int top = (height - PANEL_H) / 2;
        graphics.fill(left, top, left + PANEL_W, top + PANEL_H, 0xE0101018);
        graphics.centeredText(font, title, width / 2, top + 8, 0xFFFFFFFF);

        MinigameRenderer.render(minigame, graphics, left + 12, top + 24, sent ? 1f : partialTick);

        Component footer = switch (minigame.state()) {
            case RUNNING -> minigame.instructions();
            case SUCCESS -> Component.translatable("minigame.skillcraft.success");
            case FAILURE -> Component.translatable("minigame.skillcraft.failure");
        };
        int color = switch (minigame.state()) {
            case RUNNING -> 0xFFAAAAAA;
            case SUCCESS -> 0xFF55FF55;
            case FAILURE -> 0xFFFF5555;
        };
        graphics.centeredText(font, footer, width / 2, top + PANEL_H - 16, color);
    }

    // Space and left mouse are the action button
    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == InputConstants.KEY_SPACE) {
            if (!buttonDown) { // ignore key repeat
                buttonDown = true;
                input(true);
            }
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        if (event.key() == InputConstants.KEY_SPACE) {
            buttonDown = false;
            input(false);
            return true;
        }
        return super.keyReleased(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.input() == InputConstants.MOUSE_BUTTON_LEFT) input(true);
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.input() == InputConstants.MOUSE_BUTTON_LEFT) input(false);
        return true;
    }

    // Leaving after starting counts as a failure (the server sees an unfinished game);
    // leaving before touching anything is free
    @Override
    public void onClose() {
        if (!sent && minigame.started()) send();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
