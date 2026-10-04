package com.toddlandis.skillcraft.client;

import com.toddlandis.skillcraft.cooking.CookingRangeMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

// Cooking Range screen. Reuses the vanilla smoker art (same slot layout) and draws one extra slot
// beside the output for burnt food.
public class CookingRangeScreen extends AbstractContainerScreen<CookingRangeMenu> {
    private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/gui/container/smoker.png");
    private static final Identifier LIT_SPRITE = Identifier.withDefaultNamespace("container/smoker/lit_progress");
    private static final Identifier PROGRESS_SPRITE = Identifier.withDefaultNamespace("container/smoker/burn_progress");

    public CookingRangeScreen(CookingRangeMenu menu, Inventory inventory, Component title) {
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
        if (menu.isLit()) {
            int h = Mth.ceil(menu.fuelLeft() * 13) + 1;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, LIT_SPRITE, 14, 14, 0, 14 - h, leftPos + 56, topPos + 36 + 14 - h, 14, h);
        }
        int width = Mth.ceil(menu.progress() * 24);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, PROGRESS_SPRITE, 24, 16, 0, 0, leftPos + 79, topPos + 34, width, 16);

        // Burnt food slot, drawn like a vanilla slot
        int x = leftPos + CookingRangeMenu.BURNT_X - 1, y = topPos + CookingRangeMenu.BURNT_Y - 1;
        graphics.fill(x, y, x + 18, y + 18, 0xFF8B8B8B);
        graphics.fill(x, y, x + 17, y + 1, 0xFF373737);
        graphics.fill(x, y, x + 1, y + 17, 0xFF373737);
        graphics.fill(x + 1, y + 17, x + 18, y + 18, 0xFFFFFFFF);
        graphics.fill(x + 17, y + 1, x + 18, y + 18, 0xFFFFFFFF);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        Component burnt = Component.translatable("screen.skillcraft.range_burnt");
        graphics.text(font, burnt, CookingRangeMenu.BURNT_X + 8 - font.width(burnt) / 2, CookingRangeMenu.BURNT_Y + 19, 0xFF6A3A2A, false);
        if (menu.storedXp() > 0) {
            Component xp = Component.translatable("screen.skillcraft.smelter_xp", menu.storedXp());
            graphics.text(font, xp, imageWidth - 8 - font.width(xp), 64, 0xFF3F7F3F, false);
        }
    }
}
