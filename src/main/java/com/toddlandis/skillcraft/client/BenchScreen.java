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

// Recipe bench screen (Forging Anvil / Crafting Bench), OSRS-style:
//   left: grid of recipes (locked ones dimmed, with the level needed)
//   right: selected recipe's level, XP, time and ingredients (have/need), plus Make buttons
//   middle: the queue with a progress bar and Cancel
//   bottom: 9 output slots and the player's inventory
public class BenchScreen extends AbstractContainerScreen<BenchMenu> {
    private static final int GRID_X = 8, GRID_Y = 18, COLS = 6, ROWS = 5, CELL = 18;
    private static final int PANEL_X = 122, PANEL_W = 102;
    private static final int QUEUE_Y = 110;
    private static final int[] AMOUNTS = {1, 5, 10, 64};

    private final List<Item> recipes;
    private final List<Button> makeButtons = new ArrayList<>();
    private Button cancelButton;
    private int scrollRow;
    private int selected;

    public BenchScreen(BenchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 232, BenchMenu.INVENTORY_Y + 82);
        this.recipes = BenchRecipe.forStation(menu.station());
        this.inventoryLabelX = 35;
        this.inventoryLabelY = BenchMenu.INVENTORY_Y - 11;
    }

    @Override
    protected void init() {
        super.init();
        makeButtons.clear();
        for (int i = 0; i < AMOUNTS.length; i++) {
            int amount = AMOUNTS[i];
            Component label = amount == 64 ? Component.translatable("screen.skillcraft.bench_all") : Component.literal("×" + amount);
            Button button = Button.builder(label, b -> queue(amount))
                    .bounds(leftPos + PANEL_X + i * 25, topPos + 92, 24, 14).build();
            makeButtons.add(addRenderableWidget(button));
        }
        cancelButton = addRenderableWidget(Button.builder(Component.translatable("screen.skillcraft.bench_cancel"),
                b -> ClientPacketDistributor.sendToServer(BenchActionPayload.cancel(menu.containerId)))
                .bounds(leftPos + PANEL_X + 50, topPos + QUEUE_Y - 1, 52, 12).build());
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

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int x = leftPos, y = topPos;
        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF2B2B33);
        graphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFFC6C6C6);

        // Recipe grid
        graphics.fill(x + GRID_X - 1, y + GRID_Y - 1, x + GRID_X + COLS * CELL + 1, y + GRID_Y + ROWS * CELL + 1, 0xFF373737);
        int level = level();
        for (int i = 0; i < COLS * ROWS; i++) {
            int index = scrollRow * COLS + i;
            int cx = x + GRID_X + (i % COLS) * CELL, cy = y + GRID_Y + (i / COLS) * CELL;
            graphics.fill(cx, cy, cx + CELL - 1, cy + CELL - 1, index == selected ? 0xFF6E8B3D : 0xFF8B8B8B);
            if (index >= recipes.size()) continue;
            Item item = recipes.get(index);
            graphics.item(new ItemStack(item), cx + 1, cy + 1);
            BenchRecipe recipe = BenchRecipe.get(item);
            if (recipe != null && recipe.level() > level) {
                graphics.fill(cx, cy, cx + CELL - 1, cy + CELL - 1, 0xA0000000);
            }
        }

        // Details panel
        graphics.fill(x + PANEL_X - 1, y + GRID_Y - 1, x + PANEL_X + PANEL_W + 1, y + 108, 0xFF8B8B8B);

        // Queue bar
        int barX = x + 26, barY = y + QUEUE_Y + 3, barW = PANEL_X - 32;
        graphics.fill(barX, barY, barX + barW, barY + 5, 0xFF373737);
        graphics.fill(barX, barY, barX + (int) (barW * menu.progress()), barY + 5, 0xFF55C855);
        Item current = menu.currentItem();
        if (current != null) graphics.item(new ItemStack(current), x + 8, y + QUEUE_Y - 4);

        // Slot backgrounds
        for (Slot slot : menu.slots) {
            graphics.fill(x + slot.x - 1, y + slot.y - 1, x + slot.x + 17, y + slot.y + 17, 0xFF8B8B8B);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        Item item = selectedItem();
        BenchRecipe recipe = item == null ? null : BenchRecipe.get(item);
        if (recipe != null) {
            int px = PANEL_X + 2, py = GRID_Y + 2;
            graphics.text(font, font.substrByWidth(item.getDefaultInstance().getHoverName(), PANEL_W - 4).getString(), px, py, 0xFF202020, false);
            boolean met = level() >= recipe.level();
            graphics.text(font, Component.translatable("screen.skillcraft.bench_level", menu.station().skill().displayName(), recipe.level()),
                    px, py + 11, met ? 0xFF205020 : 0xFFA02020, false);
            graphics.text(font, Component.translatable("screen.skillcraft.bench_xp_time", XpTable.format(recipe.xp()), XpTable.format(recipe.time() / 20.0)),
                    px, py + 21, 0xFF404040, false);

            List<ItemStack> inventory = minecraft.player.getInventory().getNonEquipmentItems();
            for (int i = 0; i < recipe.ingredients().size() && i < 4; i++) {
                BenchRecipe.Part part = recipe.ingredients().get(i);
                int ix = px + (i % 2) * 50, iy = py + 32 + (i / 2) * 18;
                exampleItem(part).ifPresent(stack -> graphics.item(stack, ix, iy));
                int have = 0;
                for (ItemStack stack : inventory) if (!stack.isEmpty() && part.ingredient().test(stack)) have += stack.getCount();
                graphics.text(font, have + "/" + part.count(), ix + 18, iy + 5, have >= part.count() ? 0xFF205020 : 0xFFA02020, false);
            }
        }

        // Queue text
        if (menu.totalQueued() > 0 && menu.currentItem() != null) {
            graphics.text(font, Component.translatable("screen.skillcraft.bench_queue", menu.totalQueued()), 26, QUEUE_Y - 7, 0xFF404040, false);
        } else {
            graphics.text(font, Component.translatable("screen.skillcraft.bench_idle"), 26, QUEUE_Y - 7, 0xFF707070, false);
        }
    }

    private static Optional<ItemStack> exampleItem(BenchRecipe.Part part) {
        return part.ingredient().items().findFirst().map(Holder::value).map(ItemStack::new);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
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
            graphics.setTooltipForNextFrame(font, lines, Optional.empty(), ItemStack.EMPTY, mouseX, mouseY);
        }
    }

    // --- Input ---

    private int gridIndexAt(double mouseX, double mouseY) {
        int gx = (int) mouseX - leftPos - GRID_X, gy = (int) mouseY - topPos - GRID_Y;
        if (gx < 0 || gy < 0 || gx >= COLS * CELL || gy >= ROWS * CELL) return -1;
        int index = (scrollRow + gy / CELL) * COLS + gx / CELL;
        return index < recipes.size() ? index : -1;
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
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int totalRows = (recipes.size() + COLS - 1) / COLS;
        int maxScroll = Math.max(0, totalRows - ROWS);
        if (gridIndexAt(mouseX, mouseY) >= 0 || (mouseX - leftPos < PANEL_X && mouseY - topPos < QUEUE_Y - 8)) {
            scrollRow = Math.clamp(scrollRow - (int) Math.signum(scrollY), 0, maxScroll);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
}
