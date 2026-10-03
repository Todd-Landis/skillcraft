package com.toddlandis.skillcraft.client;

import com.toddlandis.skillcraft.smithing.SmelterMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

// Smelter screen. Reuses the vanilla blast furnace art (same slot layout) until custom art exists.
public class SmelterScreen extends AbstractContainerScreen<SmelterMenu> {
    private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/gui/container/blast_furnace.png");
    private static final Identifier LIT_SPRITE = Identifier.withDefaultNamespace("container/blast_furnace/lit_progress");
    private static final Identifier PROGRESS_SPRITE = Identifier.withDefaultNamespace("container/blast_furnace/burn_progress");

    public SmelterScreen(SmelterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = (imageWidth - font.width(title)) / 2;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        if (menu.isWorking()) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, LIT_SPRITE, 14, 14, 0, 0, leftPos + 56, topPos + 36, 14, 14);
        }
        int width = Mth.ceil(menu.progress() * 24);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, PROGRESS_SPRITE, 24, 16, 0, 0, leftPos + 79, topPos + 34, width, 16);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        if (menu.storedXp() > 0) {
            Component xp = Component.translatable("screen.skillcraft.smelter_xp", menu.storedXp());
            graphics.text(font, xp, imageWidth - 8 - font.width(xp), 64, 0xFF3F7F3F, false);
        }
    }
}
