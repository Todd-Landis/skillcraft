package com.toddlandis.skillcraft.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.toddlandis.skillcraft.bench.BenchMenu;
import com.toddlandis.skillcraft.bench.BenchRecipe;
import com.toddlandis.skillcraft.network.BenchActionPayload;
import com.toddlandis.skillcraft.skill.SkillManager;
import com.toddlandis.skillcraft.skill.XpTable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

// Recipe bench screen (Forging Anvil / Crafting Bench) with a responsive layout:
//   - Recipe panel: grid of recipes (locked ones dimmed), the selected recipe's level, XP,
//     time and ingredients (have/need), Make buttons, and the queue with Cancel.
//   - Inventory panel: the bench's 9 output slots above the player's inventory.
//   Wide windows put the two panels side by side; narrow windows stack them, and the
//   recipe grid gets as many rows as fit.
public class BenchScreen extends AbstractContainerScreen<BenchMenu> {
    private static final int MAIN_W = 232, INV_W = 176, INV_H = 132, GAP = 6, MARGIN = 4;
    private static final int COLS = 6, CELL = 18, MIN_ROWS = 3, MAX_ROWS = 8;
    private static final int PANEL_W = 102;
    private static final int DETAILS_MIN_H = 84;
    // Main panel height without the recipe area: title row, queue row, padding
    private static final int MAIN_CHROME = 16 + 6 + 16 + 4;
    private static final int[] AMOUNTS = {1, 5, 10, 64};

    private final List<Item> recipes;
    private final List<Button> makeButtons = new ArrayList<>();
    private Button cancelButton;
    private int scrollRow;
    private int selected;

    // Layout (absolute screen coordinates), recomputed on resize in init()
    private int mainX, mainY, mainH, rows, areaH;
    private int gridX, gridY, panelX, queueY;

    public BenchScreen(BenchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, INV_W, INV_H);
        this.recipes = BenchRecipe.forStation(menu.station());
        this.inventoryLabelX = 8;
        this.inventoryLabelY = BenchMenu.INVENTORY_Y - 10;
    }

    @Override
    protected void init() {
        super.init();
        layout();
        makeButtons.clear();
        for (int i = 0; i < AMOUNTS.length; i++) {
            int amount = AMOUNTS[i];
            Component label = amount == 64 ? Component.translatable("screen.skillcraft.bench_all") : Component.literal("×" + amount);
            makeButtons.add(addRenderableWidget(Button.builder(label, b -> queue(amount))
                    .bounds(panelX + i * 25, gridY + areaH - 14, 24, 14).build()));
        }
        cancelButton = addRenderableWidget(Button.builder(Component.translatable("screen.skillcraft.bench_cancel"),
                b -> ClientPacketDistributor.sendToServer(BenchActionPayload.cancel(menu.containerId)))
                .bounds(panelX + 50, queueY - 1, 52, 12).build());
    }

    // Places the recipe and inventory panels to fit the window
    private void layout() {
        boolean sideBySide = width >= MAIN_W + GAP + INV_W + MARGIN * 2;
        int available = sideBySide ? height - MARGIN * 2 : height - MARGIN * 2 - GAP - INV_H;
        rows = Math.clamp((available - MAIN_CHROME) / CELL, MIN_ROWS, MAX_ROWS);
        areaH = Math.max(rows * CELL, DETAILS_MIN_H);
        mainH = MAIN_CHROME + areaH;
        if (sideBySide) {
            mainX = (width - (MAIN_W + GAP + INV_W)) / 2;
            mainY = Math.max(MARGIN, (height - Math.max(mainH, INV_H)) / 2);
            leftPos = mainX + MAIN_W + GAP;
            topPos = mainY;
        } else {
            mainX = (width - MAIN_W) / 2;
            mainY = Math.max(MARGIN, (height - (mainH + GAP + INV_H)) / 2);
            leftPos = (width - INV_W) / 2;
            topPos = mainY + mainH + GAP;
        }
        gridX = mainX + 8;
        gridY = mainY + 16;
        panelX = mainX + 122;
        queueY = gridY + areaH + 8;
    }

    private Item selectedItem() {
        return selected >= 0 && selected < recipes.size() ? recipes.get(selected) : null;
    }

    private int level() {
        return SkillManager.level(minecraft.player, menu.station().skill());
    }

    private void queue(int amount) {
        Item item = selectedItem();
        if (item == null) return;
        ClientPacketDistributor.sendToServer(new BenchActionPayload(menu.containerId, BuiltInRegistries.ITEM.getKey(item), amount));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        Item item = selectedItem();
        BenchRecipe recipe = item == null ? null : BenchRecipe.get(item);
        boolean canMake = recipe != null && level() >= recipe.level()
                && recipe.maxCrafts(minecraft.player.getInventory().getNonEquipmentItems()) > 0;
        makeButtons.forEach(b -> b.active = canMake);
        cancelButton.active = menu.totalQueued() > 0;
    }

    // --- Drawing ---

    private static void frame(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, 0xFF2B2B33);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFFC6C6C6);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        frame(g, mainX, mainY, MAIN_W, mainH);
        frame(g, leftPos, topPos, INV_W, INV_H);
        g.text(font, title, mainX + 8, mainY + 5, 0xFF404040, false);

        // Recipe grid
        g.fill(gridX - 1, gridY - 1, gridX + COLS * CELL + 1, gridY + rows * CELL + 1, 0xFF373737);
        int level = level();
        for (int i = 0; i < COLS * rows; i++) {
            int index = scrollRow * COLS + i;
            int cx = gridX + (i % COLS) * CELL, cy = gridY + (i / COLS) * CELL;
            g.fill(cx, cy, cx + CELL - 1, cy + CELL - 1, index == selected ? 0xFF6E8B3D : 0xFF8B8B8B);
            if (index >= recipes.size()) continue;
            Item item = recipes.get(index);
            g.item(new ItemStack(item), cx + 1, cy + 1);
            BenchRecipe recipe = BenchRecipe.get(item);
            if (recipe != null && recipe.level() > level) g.fill(cx, cy, cx + CELL - 1, cy + CELL - 1, 0xA0000000);
        }

        // Details panel
        g.fill(panelX - 1, gridY - 1, panelX + PANEL_W + 1, gridY + areaH + 1, 0xFF8B8B8B);
        drawDetails(g);

        // Queue
        int barX = mainX + 26, barY = queueY + 3, barW = panelX - mainX - 32;
        g.fill(barX, barY, barX + barW, barY + 5, 0xFF373737);
        g.fill(barX, barY, barX + (int) (barW * menu.progress()), barY + 5, 0xFF55C855);
        Item current = menu.currentItem();
        if (current != null) g.item(new ItemStack(current), mainX + 6, queueY - 2);
        Component queueText = menu.totalQueued() > 0 && current != null
                ? Component.translatable("screen.skillcraft.bench_queue", menu.totalQueued())
                : Component.translatable("screen.skillcraft.bench_idle");
        g.text(font, queueText, barX, queueY - 7, 0xFF404040, false);

        // Slot backgrounds
        for (Slot slot : menu.slots) {
            g.fill(leftPos + slot.x - 1, topPos + slot.y - 1, leftPos + slot.x + 17, topPos + slot.y + 17, 0xFF8B8B8B);
        }
    }

    private void drawDetails(GuiGraphicsExtractor g) {
        Item item = selectedItem();
        BenchRecipe recipe = item == null ? null : BenchRecipe.get(item);
        if (recipe == null) return;
        int px = panelX + 2, py = gridY + 2;
        g.text(font, font.substrByWidth(item.getDefaultInstance().getHoverName(), PANEL_W - 4).getString(), px, py, 0xFF202020, false);
        boolean met = level() >= recipe.level();
        g.text(font, Component.translatable("screen.skillcraft.bench_level", menu.station().skill().displayName(), recipe.level()),
                px, py + 11, met ? 0xFF205020 : 0xFFA02020, false);
        g.text(font, Component.translatable("screen.skillcraft.bench_xp_time", XpTable.format(recipe.xp()), XpTable.format(recipe.time() / 20.0)),
                px, py + 21, 0xFF404040, false);

        List<ItemStack> inventory = minecraft.player.getInventory().getNonEquipmentItems();
        for (int i = 0; i < recipe.ingredients().size() && i < 4; i++) {
            BenchRecipe.Part part = recipe.ingredients().get(i);
            int ix = px + (i % 2) * 50, iy = py + 32 + (i / 2) * 18;
            exampleItem(part).ifPresent(stack -> g.item(stack, ix, iy));
            int have = 0;
            for (ItemStack stack : inventory) if (!stack.isEmpty() && part.ingredient().test(stack)) have += stack.getCount();
            g.text(font, have + "/" + part.count(), ix + 18, iy + 5, have >= part.count() ? 0xFF205020 : 0xFFA02020, false);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(font, Component.translatable("screen.skillcraft.bench_output"), 8, 6, 0xFF404040, false);
        g.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFF404040, false);
    }

    private static Optional<ItemStack> exampleItem(BenchRecipe.Part part) {
        return part.ingredient().items().findFirst().map(Holder::value).map(ItemStack::new);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractTooltip(g, mouseX, mouseY);
        int index = gridIndexAt(mouseX, mouseY);
        if (index >= 0) {
            Item item = recipes.get(index);
            BenchRecipe recipe = BenchRecipe.get(item);
            boolean met = recipe != null && level() >= recipe.level();
            List<Component> lines = new ArrayList<>();
            lines.add(item.getDefaultInstance().getHoverName());
            if (recipe != null) {
                lines.add(Component.translatable("screen.skillcraft.bench_level", menu.station().skill().displayName(), recipe.level())
                        .withStyle(met ? ChatFormatting.GREEN : ChatFormatting.RED));
            }
            g.setTooltipForNextFrame(font, lines, Optional.empty(), ItemStack.EMPTY, mouseX, mouseY);
        }
    }

    // --- Input ---

    private boolean inMainPanel(double mx, double my) {
        return mx >= mainX && my >= mainY && mx < mainX + MAIN_W && my < mainY + mainH;
    }

    private int gridIndexAt(double mx, double my) {
        int gx = (int) mx - gridX, gy = (int) my - gridY;
        if (gx < 0 || gy < 0 || gx >= COLS * CELL || gy >= rows * CELL) return -1;
        int index = (scrollRow + gy / CELL) * COLS + gx / CELL;
        return index < recipes.size() ? index : -1;
    }

    @Override
    protected boolean hasClickedOutside(double mx, double my, int xo, int yo) {
        return super.hasClickedOutside(mx, my, xo, yo) && !inMainPanel(mx, my);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int index = gridIndexAt(event.x(), event.y());
        if (index >= 0) {
            selected = index;
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        if (inMainPanel(mx, my)) {
            int maxScroll = Math.max(0, (recipes.size() + COLS - 1) / COLS - rows);
            scrollRow = Math.clamp(scrollRow - (int) Math.signum(scrollY), 0, maxScroll);
            return true;
        }
        return super.mouseScrolled(mx, my, scrollX, scrollY);
    }
}
