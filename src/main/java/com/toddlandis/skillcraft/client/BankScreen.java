package com.toddlandis.skillcraft.client;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.mojang.blaze3d.platform.InputConstants;
import com.toddlandis.skillcraft.bank.BankActions;
import com.toddlandis.skillcraft.bank.BankMenu;
import com.toddlandis.skillcraft.bank.PlayerBank;
import com.toddlandis.skillcraft.network.BankActionPayload;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

// OSRS-style bank interface.
//   - 8-column item grid with OSRS count colors (yellow / white "K" / green "M"), scrolling
//   - Tabs: "All" + up to 9 tabs; drag an item onto a tab to file it, onto "+" for a new tab
//   - Rearrange mode Swap/Insert: drag items to reorder
//   - Quantity 1/5/10/X/All for left-clicks; right-click for Withdraw-/Deposit- options
//   - Click inventory items to deposit; Deposit inventory / Deposit worn items buttons
//   - Search filters by item name
public class BankScreen extends AbstractContainerScreen<BankMenu> {
    // OSRS palette
    private static final int FRAME_DARK = 0xFF1E1913;
    private static final int FRAME_LIGHT = 0xFF6E5E44;
    private static final int PANEL = 0xFF3E3529;
    private static final int PANEL_INNER = 0xFF4A4034;
    private static final int BUTTON = 0xFF5A4D3A;
    private static final int BUTTON_ON = 0xFF7A3B2A;
    private static final int ORANGE = 0xFFFF981F;
    private static final int YELLOW = 0xFFFFFF00;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int GREEN = 0xFF00FF80;

    private static final int GRID_X = 8, GRID_Y = 38, COLS = 8, ROWS = 5, CELL = 20;
    private static final int TAB_X = 4, TAB_Y = 16, TAB_W = 16, TAB_H = 19;
    private static final int ROW1_Y = 144, ROW2_Y = 160;
    private static final int SCROLL_X = 170;

    static final int[] QUANTITIES = {1, 5, 10, 0, BankActions.ALL}; // 0 = X
    private static final String[] QUANTITY_LABELS = {"1", "5", "10", "X", "All"};

    // Simple OSRS-looking buttons drawn by hand
    private record Button(int x, int y, int w, int h, String key, Runnable action) {
        boolean hit(double mx, double my) { return mx >= x && my >= y && mx < x + w && my < y + h; }
    }

    // Right-click "Choose Option" menu
    private record MenuOption(Component label, Runnable action) {}

    private final List<Button> buttons = new ArrayList<>();
    private List<MenuOption> contextMenu;
    private Component contextTitle;
    private int contextX, contextY;

    private EditBox search;
    private boolean searching;
    private EditBox amountBox;
    private java.util.function.IntConsumer pendingX;

    private int scrollRow;
    private int pressIndex = -1;
    private double pressX, pressY;

    public BankScreen(BankMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 184, BankMenu.INVENTORY_Y + 82);
        this.inventoryLabelX = BankMenu.INVENTORY_X;
        this.inventoryLabelY = BankMenu.INVENTORY_Y - 11;
    }

    @Override
    protected void init() {
        super.init();
        buttons.clear();
        int x = leftPos, y = topPos;
        buttons.add(new Button(x + 8, y + ROW1_Y, 32, 13, "screen.skillcraft.bank_swap", () -> ClientBank.insertMode = false));
        buttons.add(new Button(x + 42, y + ROW1_Y, 34, 13, "screen.skillcraft.bank_insert", () -> ClientBank.insertMode = true));
        buttons.add(new Button(x + 104, y + ROW1_Y, 22, 13, "screen.skillcraft.bank_search", this::toggleSearch));
        buttons.add(new Button(x + 128, y + ROW1_Y, 24, 13, "screen.skillcraft.bank_deposit_inv", () -> send(BankActions.Action.DEPOSIT_INVENTORY, 0, 0, depositTab())));
        buttons.add(new Button(x + 154, y + ROW1_Y, 22, 13, "screen.skillcraft.bank_deposit_worn", () -> send(BankActions.Action.DEPOSIT_WORN, 0, 0, depositTab())));
        for (int i = 0; i < QUANTITIES.length; i++) {
            int mode = i;
            buttons.add(new Button(x + 50 + i * 25, y + ROW2_Y, 24, 13, null, () -> {
                ClientBank.quantityMode = mode;
                if (QUANTITIES[mode] == 0) askAmount(amount -> {}); // set X now, like OSRS
            }));
        }

        search = new EditBox(font, x + 34, y + 3, 116, 11, Component.translatable("screen.skillcraft.bank_search"));
        search.setMaxLength(40);
        search.setVisible(searching);
        search.setResponder(text -> scrollRow = 0);
        addRenderableWidget(search);

        amountBox = new EditBox(font, x + imageWidth / 2 - 40, y + 82, 80, 12, Component.translatable("screen.skillcraft.bank_enter_amount"));
        amountBox.setMaxLength(12);
        amountBox.setVisible(false); // drawn and fed input by hand so it sits above everything
    }

    // --- Helpers ---

    private int depositTab() {
        return ClientBank.currentTab;
    }

    private void send(BankActions.Action action, int index, int amount, int target) {
        ClientPacketDistributor.sendToServer(new BankActionPayload(menu.containerId, action.ordinal(), index, amount, target));
    }

    // Bank entry indices shown in the grid, after tab and search filters
    private List<Integer> visible() {
        List<Integer> result = new ArrayList<>();
        String filter = searching ? search.getValue().toLowerCase(Locale.ROOT).trim() : "";
        for (int i = 0; i < ClientBank.entries.size(); i++) {
            PlayerBank.Entry entry = ClientBank.entries.get(i);
            if (ClientBank.currentTab != 0 && entry.tab() != ClientBank.currentTab) continue;
            if (!filter.isEmpty() && !entry.item().getHoverName().getString().toLowerCase(Locale.ROOT).contains(filter)) continue;
            result.add(i);
        }
        return result;
    }

    private int maxScroll() {
        return Math.max(0, (visible().size() + COLS - 1) / COLS - ROWS);
    }

    // Grid cell under the mouse -> bank entry index, or -1
    private int entryAt(double mx, double my) {
        int gx = (int) mx - leftPos - GRID_X, gy = (int) my - topPos - GRID_Y;
        if (gx < 0 || gy < 0 || gx >= COLS * CELL || gy >= ROWS * CELL) return -1;
        int slot = (scrollRow + gy / CELL) * COLS + gx / CELL;
        List<Integer> shown = visible();
        return slot < shown.size() ? shown.get(slot) : -1;
    }

    // Tab under the mouse: 0 = All, 1-9 = tabs, 10 = "+" (new tab), -1 = none
    private int tabAt(double mx, double my) {
        int tx = (int) mx - leftPos - TAB_X, ty = (int) my - topPos - TAB_Y;
        if (tx < 0 || ty < 0 || ty >= TAB_H) return -1;
        int tab = tx / TAB_W;
        int shownTabs = highestTab() + 1; // "All" plus every tab in use
        if (tab < shownTabs) return tab;
        if (tab == shownTabs && shownTabs <= PlayerBank.MAX_TABS) return 10;
        return -1;
    }

    private int highestTab() {
        int max = 0;
        for (PlayerBank.Entry entry : ClientBank.entries) max = Math.max(max, entry.tab());
        return max;
    }

    private void toggleSearch() {
        searching = !searching;
        search.setVisible(searching);
        search.setValue("");
        setFocused(searching ? search : null);
        search.setFocused(searching);
        scrollRow = 0;
    }

    private void askAmount(java.util.function.IntConsumer then) {
        pendingX = then;
        amountBox.setValue("");
        amountBox.setVisible(true);
        setFocused(amountBox);
        amountBox.setFocused(true);
    }

    private void confirmAmount() {
        int amount = parseAmount(amountBox.getValue());
        amountBox.setVisible(false);
        amountBox.setFocused(false);
        if (amount > 0) {
            ClientBank.lastX = amount;
            if (pendingX != null) pendingX.accept(amount);
        }
        pendingX = null;
    }

    // Accepts "250", "10k", "1.5m", "2b" like OSRS
    static int parseAmount(String text) {
        try {
            String s = text.trim().toLowerCase(Locale.ROOT);
            double mult = 1;
            if (s.endsWith("k")) { mult = 1e3; s = s.substring(0, s.length() - 1); }
            else if (s.endsWith("m")) { mult = 1e6; s = s.substring(0, s.length() - 1); }
            else if (s.endsWith("b")) { mult = 1e9; s = s.substring(0, s.length() - 1); }
            return (int) Math.min(Integer.MAX_VALUE, Math.max(0, Double.parseDouble(s) * mult));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private int currentQuantity() {
        int q = QUANTITIES[ClientBank.quantityMode];
        return q == 0 ? ClientBank.lastX : q;
    }

    // OSRS bank count text and color
    static String countText(int count) {
        if (count < 100_000) return String.valueOf(count);
        if (count < 10_000_000) return (count / 1000) + "K";
        return (count / 1_000_000) + "M";
    }

    static int countColor(int count) {
        if (count < 100_000) return YELLOW;
        if (count < 10_000_000) return WHITE;
        return GREEN;
    }

    // --- Drawing ---

    private void bevel(GuiGraphicsExtractor g, int x, int y, int w, int h, int fill) {
        g.fill(x, y, x + w, y + h, FRAME_DARK);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, FRAME_LIGHT);
        g.fill(x + 2, y + 2, x + w - 1, y + h - 1, fill);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        int x = leftPos, y = topPos;
        bevel(g, x, y, imageWidth, imageHeight, PANEL);

        // Tabs
        int shownTabs = highestTab() + 1;
        for (int t = 0; t <= Math.min(shownTabs, PlayerBank.MAX_TABS); t++) {
            int tx = x + TAB_X + t * TAB_W;
            boolean plus = t == shownTabs;
            boolean active = !plus && t == ClientBank.currentTab;
            bevel(g, tx, y + TAB_Y, TAB_W, TAB_H, active ? BUTTON_ON : BUTTON);
            if (plus) {
                g.centeredText(font, "+", tx + TAB_W / 2, y + TAB_Y + 6, ORANGE);
            } else if (t == 0) {
                g.item(new ItemStack(Items.CHEST), tx, y + TAB_Y + 2);
            } else {
                for (PlayerBank.Entry entry : ClientBank.entries) {
                    if (entry.tab() == t) { g.item(entry.item(), tx, y + TAB_Y + 2); break; }
                }
            }
        }

        // Grid
        bevel(g, x + GRID_X - 2, y + GRID_Y - 2, COLS * CELL + 4, ROWS * CELL + 4, PANEL_INNER);
        List<Integer> shown = visible();
        for (int i = 0; i < COLS * ROWS; i++) {
            int slot = scrollRow * COLS + i;
            if (slot >= shown.size()) break;
            PlayerBank.Entry entry = ClientBank.entries.get(shown.get(slot));
            int cx = x + GRID_X + (i % COLS) * CELL, cy = y + GRID_Y + (i / COLS) * CELL;
            boolean dragged = pressIndex == shown.get(slot) && isDragging(mouseX, mouseY);
            if (!dragged) drawEntry(g, entry, cx + 2, cy + 3);
        }

        // Scrollbar
        int max = maxScroll();
        g.fill(x + SCROLL_X, y + GRID_Y, x + SCROLL_X + 6, y + GRID_Y + ROWS * CELL, FRAME_DARK);
        int thumbH = Math.max(10, ROWS * CELL * ROWS / Math.max(ROWS, ROWS + max));
        int thumbY = max == 0 ? 0 : (ROWS * CELL - thumbH) * scrollRow / max;
        g.fill(x + SCROLL_X + 1, y + GRID_Y + thumbY, x + SCROLL_X + 5, y + GRID_Y + thumbY + thumbH, FRAME_LIGHT);

        // Buttons
        for (int i = 0; i < buttons.size(); i++) {
            Button b = buttons.get(i);
            boolean on = (i == 0 && !ClientBank.insertMode) || (i == 1 && ClientBank.insertMode) || (i == 2 && searching)
                    || (i >= 5 && i - 5 == ClientBank.quantityMode);
            bevel(g, b.x, b.y, b.w, b.h, on ? BUTTON_ON : BUTTON);
        }

        // Inventory slot backgrounds
        for (Slot slot : menu.slots) {
            g.fill(x + slot.x - 1, y + slot.y - 1, x + slot.x + 17, y + slot.y + 17, PANEL_INNER);
        }
    }

    private void drawEntry(GuiGraphicsExtractor g, PlayerBank.Entry entry, int ix, int iy) {
        g.item(entry.item(), ix, iy);
        g.itemDecorations(font, entry.item(), ix, iy, "");
        if (entry.count() > 1 || entry.item().getMaxStackSize() > 1) {
            g.pose().pushMatrix();
            g.pose().translate(ix - 2, iy - 3);
            g.pose().scale(0.75f, 0.75f);
            g.text(font, countText(entry.count()), 0, 0, countColor(entry.count()), true);
            g.pose().popMatrix();
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        if (!searching) g.centeredText(font, title, imageWidth / 2, 5, ORANGE);
        g.text(font, ClientBank.entries.size() + "", 6, 5, YELLOW, true);
        g.text(font, Component.translatable("screen.skillcraft.bank_quantity"), 8, ROW2_Y + 3, ORANGE, true);
        g.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, ORANGE, true);
        for (int i = 0; i < buttons.size(); i++) {
            Button b = buttons.get(i);
            String label = b.key != null ? Component.translatable(b.key).getString() : QUANTITY_LABELS[i - 5];
            if (i >= 5 && QUANTITIES[i - 5] == 0) label = "X";
            g.centeredText(font, label, b.x - leftPos + b.w / 2, b.y - topPos + 3, ORANGE);
        }
    }

    private boolean isDragging(double mx, double my) {
        return pressIndex >= 0 && (Math.abs(mx - pressX) > 4 || Math.abs(my - pressY) > 4);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);

        // Dragged item follows the mouse
        if (isDragging(mouseX, mouseY) && pressIndex < ClientBank.entries.size()) {
            g.nextStratum();
            drawEntry(g, ClientBank.entries.get(pressIndex), mouseX - 8, mouseY - 8);
        }

        // "Enter amount" prompt
        if (amountBox.isVisible()) {
            g.nextStratum();
            int bx = leftPos + imageWidth / 2 - 50;
            bevel(g, bx, topPos + 64, 100, 38, PANEL);
            g.centeredText(font, Component.translatable("screen.skillcraft.bank_enter_amount"), leftPos + imageWidth / 2, topPos + 70, ORANGE);
            amountBox.extractRenderState(g, mouseX, mouseY, partialTick);
        }

        // Right-click menu
        if (contextMenu != null) {
            g.nextStratum();
            int w = font.width(contextTitle) + 8;
            for (MenuOption option : contextMenu) w = Math.max(w, font.width(option.label()) + 8);
            int h = 12 + contextMenu.size() * 12;
            g.fill(contextX, contextY, contextX + w, contextY + h, 0xFF000000);
            g.fill(contextX + 1, contextY + 1, contextX + w - 1, contextY + h - 1, 0xFF5D5447);
            g.fill(contextX + 1, contextY + 1, contextX + w - 1, contextY + 11, 0xFF000000);
            g.text(font, Component.translatable("screen.skillcraft.bank_choose_option"), contextX + 3, contextY + 2, 0xFF5D5447, false);
            for (int i = 0; i < contextMenu.size(); i++) {
                int oy = contextY + 13 + i * 12;
                boolean hover = mouseX >= contextX && mouseX < contextX + w && mouseY >= oy - 1 && mouseY < oy + 11;
                g.text(font, contextMenu.get(i).label(), contextX + 4, oy, hover ? YELLOW : WHITE, true);
            }
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        if (contextMenu != null || amountBox.isVisible() || pressIndex >= 0) return;
        int index = entryAt(mouseX, mouseY);
        if (index >= 0) {
            PlayerBank.Entry entry = ClientBank.entries.get(index);
            List<Component> lines = new ArrayList<>(getTooltipFromContainerItem(entry.item()));
            lines.add(Component.translatable("screen.skillcraft.bank_amount", new DecimalFormat("#,###").format(entry.count()))
                    .withStyle(ChatFormatting.YELLOW));
            g.setTooltipForNextFrame(font, lines, entry.item().getTooltipImage(), entry.item(), mouseX, mouseY);
            return;
        }
        super.extractTooltip(g, mouseX, mouseY);
    }

    // --- Input ---

    private Component nameOf(ItemStack stack) {
        return stack.getHoverName().copy().withStyle(ChatFormatting.GOLD);
    }

    private void openWithdrawMenu(int index, double mx, double my) {
        PlayerBank.Entry entry = ClientBank.entries.get(index);
        List<MenuOption> options = new ArrayList<>();
        for (int amount : new int[]{1, 5, 10}) {
            options.add(new MenuOption(Component.translatable("screen.skillcraft.bank_withdraw_n", amount), () -> send(BankActions.Action.WITHDRAW, index, amount, 0)));
        }
        options.add(new MenuOption(Component.translatable("screen.skillcraft.bank_withdraw_n", ClientBank.lastX), () -> send(BankActions.Action.WITHDRAW, index, ClientBank.lastX, 0)));
        options.add(new MenuOption(Component.translatable("screen.skillcraft.bank_withdraw_x"), () -> askAmount(amount -> send(BankActions.Action.WITHDRAW, index, amount, 0))));
        options.add(new MenuOption(Component.translatable("screen.skillcraft.bank_withdraw_all"), () -> send(BankActions.Action.WITHDRAW, index, BankActions.ALL, 0)));
        options.add(new MenuOption(Component.translatable("screen.skillcraft.bank_withdraw_all_but_one"), () -> send(BankActions.Action.WITHDRAW, index, BankActions.ALL_BUT_ONE, 0)));
        openMenu(options, nameOf(entry.item()), mx, my);
    }

    private void openDepositMenu(int slotIndex, ItemStack stack, double mx, double my) {
        List<MenuOption> options = new ArrayList<>();
        for (int amount : new int[]{1, 5, 10}) {
            options.add(new MenuOption(Component.translatable("screen.skillcraft.bank_deposit_n", amount), () -> send(BankActions.Action.DEPOSIT, slotIndex, amount, depositTab())));
        }
        options.add(new MenuOption(Component.translatable("screen.skillcraft.bank_deposit_x"), () -> askAmount(amount -> send(BankActions.Action.DEPOSIT, slotIndex, amount, depositTab()))));
        options.add(new MenuOption(Component.translatable("screen.skillcraft.bank_deposit_all"), () -> send(BankActions.Action.DEPOSIT, slotIndex, BankActions.ALL, depositTab())));
        openMenu(options, nameOf(stack), mx, my);
    }

    private void openMenu(List<MenuOption> options, Component title, double mx, double my) {
        contextMenu = options;
        contextTitle = title;
        contextX = (int) mx - 10;
        contextY = (int) my - 4;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x(), my = event.y();
        boolean right = event.input() == InputConstants.MOUSE_BUTTON_RIGHT;

        if (contextMenu != null) {
            int w = font.width(contextTitle) + 8;
            for (MenuOption option : contextMenu) w = Math.max(w, font.width(option.label()) + 8);
            for (int i = 0; i < contextMenu.size(); i++) {
                int oy = contextY + 13 + i * 12;
                if (mx >= contextX && mx < contextX + w && my >= oy - 1 && my < oy + 11) {
                    Runnable action = contextMenu.get(i).action();
                    contextMenu = null;
                    action.run();
                    return true;
                }
            }
            contextMenu = null;
            return true;
        }
        if (amountBox.isVisible()) {
            return amountBox.mouseClicked(event, doubleClick) || true;
        }
        if (searching && search.mouseClicked(event, doubleClick)) return true;

        for (Button b : buttons) {
            if (b.hit(mx, my)) {
                b.action.run();
                return true;
            }
        }

        int tab = tabAt(mx, my);
        if (tab >= 0 && tab <= PlayerBank.MAX_TABS) {
            ClientBank.currentTab = tab;
            scrollRow = 0;
            return true;
        }

        int index = entryAt(mx, my);
        if (index >= 0) {
            if (right) {
                openWithdrawMenu(index, mx, my);
            } else {
                pressIndex = index;
                pressX = mx;
                pressY = my;
            }
            return true;
        }

        Slot slot = getHoveredSlot();
        if (slot != null) {
            if (slot.hasItem()) {
                if (right) {
                    openDepositMenu(slot.index, slot.getItem(), mx, my);
                } else if (QUANTITIES[ClientBank.quantityMode] == 0) {
                    send(BankActions.Action.DEPOSIT, slot.index, ClientBank.lastX, depositTab());
                } else {
                    send(BankActions.Action.DEPOSIT, slot.index, currentQuantity(), depositTab());
                }
            }
            return true; // never pick up items in the bank screen
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (pressIndex < 0) return super.mouseReleased(event);
        double mx = event.x(), my = event.y();
        int from = pressIndex;
        boolean dragged = isDragging(mx, my);
        pressIndex = -1;
        if (!dragged) {
            send(BankActions.Action.WITHDRAW, from, currentQuantity(), 0);
            return true;
        }
        int tab = tabAt(mx, my);
        if (tab == 10) {
            send(BankActions.Action.SET_TAB, from, 0, Math.min(highestTab() + 1, PlayerBank.MAX_TABS));
        } else if (tab >= 0) {
            send(BankActions.Action.SET_TAB, from, 0, tab);
        } else {
            int to = entryAt(mx, my);
            if (to >= 0 && to != from) {
                send(ClientBank.insertMode ? BankActions.Action.INSERT : BankActions.Action.SWAP, from, 0, to);
            }
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        scrollRow = Math.clamp(scrollRow - (int) Math.signum(scrollY), 0, maxScroll());
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (amountBox.isVisible()) {
            if (event.key() == InputConstants.KEY_RETURN || event.key() == InputConstants.KEY_NUMPADENTER) {
                confirmAmount();
                return true;
            }
            if (event.isEscape()) {
                amountBox.setVisible(false);
                pendingX = null;
                return true;
            }
            amountBox.keyPressed(event);
            return true;
        }
        if (contextMenu != null && event.isEscape()) {
            contextMenu = null;
            return true;
        }
        if (searching && search.isFocused()) {
            if (event.isEscape()) {
                toggleSearch();
                return true;
            }
            search.keyPressed(event);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (amountBox.isVisible()) return amountBox.charTyped(event);
        if (searching && search.isFocused()) return search.charTyped(event);
        return super.charTyped(event);
    }

    @Override
    public void containerTick() {
        super.containerTick();
        scrollRow = Math.clamp(scrollRow, 0, maxScroll());
        if (ClientBank.currentTab > highestTab()) ClientBank.currentTab = 0;
    }
}
