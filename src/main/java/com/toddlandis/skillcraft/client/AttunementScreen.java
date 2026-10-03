package com.toddlandis.skillcraft.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.toddlandis.skillcraft.altar.AttunementAltarBlock;
import com.toddlandis.skillcraft.client.minigame.Minigame;
import com.toddlandis.skillcraft.client.minigame.Minigames;
import com.toddlandis.skillcraft.network.AttunementResultPayload;
import com.toddlandis.skillcraft.node.NodeType;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

// Hosts whichever minigame the node uses (see client/minigame). Sends the result to the server
// when the minigame ends, shows the outcome briefly, then closes.
public class AttunementScreen extends Screen {
    private static final int RESULT_DISPLAY_TICKS = 30;
    private static final int PANEL_W = Minigame.WIDTH + 24;
    private static final int PANEL_H = Minigame.HEIGHT + 52;

    private final BlockPos altarPos;
    private final Minigame minigame;
    private boolean sent;
    private int closeTimer;
    private boolean buttonDown;

    public AttunementScreen(BlockPos altarPos, NodeType node) {
        super(Component.translatable("screen.skillcraft.attunement", AttunementAltarBlock.nodeName(node)));
        this.altarPos = altarPos;
        this.minigame = Minigames.create(node.minigame(), node.tierIndex());
    }

    @Override
    public void tick() {
        if (!sent) {
            minigame.tick();
            if (minigame.state() != Minigame.State.RUNNING) sendResult(minigame.state() == Minigame.State.SUCCESS);
        } else if (++closeTimer >= RESULT_DISPLAY_TICKS) {
            super.onClose();
        }
    }

    private void sendResult(boolean success) {
        sent = true;
        ClientPacketDistributor.sendToServer(new AttunementResultPayload(altarPos, success));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        int left = (width - PANEL_W) / 2;
        int top = (height - PANEL_H) / 2;
        graphics.fill(left, top, left + PANEL_W, top + PANEL_H, 0xE0101018);
        graphics.centeredText(font, title, width / 2, top + 8, 0xFFFFFFFF);

        minigame.render(graphics, font, left + 12, top + 24, partialTick);

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
                if (!sent) minigame.press();
            }
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        if (event.key() == InputConstants.KEY_SPACE) {
            buttonDown = false;
            if (!sent) minigame.release();
            return true;
        }
        return super.keyReleased(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.input() == InputConstants.MOUSE_BUTTON_LEFT && !sent) minigame.press();
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.input() == InputConstants.MOUSE_BUTTON_LEFT && !sent) minigame.release();
        return true;
    }

    // Leaving after starting counts as a failure; leaving before touching anything is free
    @Override
    public void onClose() {
        if (!sent && minigame.started()) sendResult(false);
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
