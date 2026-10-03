package com.toddlandis.skillcraft.client;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.IntConsumer;

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

// OSRS-style bank interface with a responsive layout:
//   - Wide windows: bank panel on the left, inventory panel on the right (like OSRS).
//   - Narrow windows: bank above inventory.
//   - The item grid gets as many rows as fit (3-12).
// Controls:
//   - Left-click a bank item: withdraw the selected quantity. Right-click: Withdraw- options.
//   - Drag a bank item onto another (Swap/Insert), onto a tab, onto "+" (new tab), or onto
//     your inventory (withdraw).
//   - Inventory works normally. Shift-click deposits (Shift+right-click: Deposit- options).
//     Clicking a held item onto the bank grid deposits it (right-click: just one).
//   - Quantity 1/5/10/X/All, Find (search), Inv (deposit inventory), Worn (deposit armor).
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

    // Panel sizes
    private static final int BANK_W = 184, INV_W = 176, INV_H = 100, GAP = 6, MARGIN = 4;
    private static final int COLS = 8, CELL = 20, MIN_ROWS = 3, MAX_ROWS = 12;
    private static final int TAB_W = 16, TAB_H = 19;
    // Bank panel height without the grid rows: title, tabs, grid frame, two control rows
    private static final int BANK_CHROME = 38 + 6 + 34 + 4;

    static final int[] QUANTITIES = {1, 5, 10, 0, BankActions.ALL}; // 0 = X
    private static final String[] QUANTITY_LABELS = {"1", "5", "10", "X", "All"};

    private record Button(int x, int y, int w, int h, String label, Runnable action) {
        boolean hit(double mx, double my) { return mx >= x && my >= y && mx < x + w && my < y + h; }
    }

    private record MenuOption(Component label, Runnable action) {}

    // Layout (absolute screen coordinates), recomputed on every resize in init()
    private int bankX, bankY, bankH, rows;
    private int gridX, gridY, tabY, row1Y, row2Y, scrollX;

    private final List<Button> buttons = new ArrayList<>();
    private List<MenuOption> contextMenu;
    private Component contextTitle;
    private int contextX, contextY;

    private EditBox search;
    private boolean searching;
    private EditBox amountBox;
    private IntConsumer pendingX;

    private int scrollRow;
    private int pressIndex = -1;
    private double pressX, pressY;

    public BankScreen(BankMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, INV_W, INV_H);
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 6;
    }

    @Override
    protected void init() {
        super.init();
        layout();
        int x = bankX, y = bankY;

        buttons.clear();
        buttons.add(new Button(x + 8, row1Y, 32, 13, "screen.skillcraft.bank_swap", () -> ClientBank.insertMode = false));
        buttons.add(new Button(x + 42, row1Y, 34, 13, "screen.skillcraft.bank_insert", () -> ClientBank.insertMode = true));
        buttons.add(new Button(x + 104, row1Y, 22, 13, "screen.skillcraft.bank_search", this::toggleSearch));
        buttons.add(new Button(x + 128, row1Y, 24, 13, "screen.skillcraft.bank_deposit_inv", () -> send(BankActions.Action.DEPOSIT_INVENTORY, 0, 0, ClientBank.currentTab)));
        buttons.add(new Button(x + 154, row1Y, 22, 13, "screen.skillcraft.bank_deposit_worn", () -> send(BankActions.Action.DEPOSIT_WORN, 0, 0, ClientBank.currentTab)));
        for (int i = 0; i < QUANTITIES.length; i++) {
            int mode = i;
            buttons.add(new Button(x + 50 + i * 25, row2Y, 24, 13, null, () -> {
                ClientBank.quantityMode = mode;
                if (QUANTITIES[mode] == 0) askAmount(amount -> {}); // set X now, like OSRS
            }));
        }

        String previousSearch = search == null ? "" : search.getValue();
        search = new EditBox(font, x + 34, y + 3, 116, 11, Component.translatable("screen.skillcraft.bank_search"));
        search.setMaxLength(40);
        search.setValue(previousSearch);
        search.setVisible(searching);
        search.setResponder(text -> scrollRow = 0);
        addRenderableWidget(search);

        amountBox = new EditBox(font, x + BANK_W / 2 - 40, y + 82, 80, 12, Component.translatable("screen.skillcraft.bank_enter_amount"));
        amountBox.setMaxLength(12);
        amountBox.setVisible(false); // drawn and fed input by hand so it sits above everything
    }

    // Places the bank and inventory panels to fit the window
    private void layout() {
        boolean sideBySide = width >= BANK_W + GAP + INV_W + MARGIN * 2;
        if (sideBySide) {
            rows = clampRows((height - MARGIN * 2 - BANK_CHROME) / CELL);
            bankH = BANK_CHROME + rows * CELL;
            bankX = (width - (BANK_W + GAP + INV_W)) / 2;
            bankY = Math.max(MARGIN, (height - bankH) / 2);
            leftPos = bankX + BANK_W + GAP;
            topPos = bankY + bankH - INV_H; // inventory lines up with the bottom, like OSRS
        } else {
            rows = clampRows((height - MARGIN * 2 - GAP - INV_H - BANK_CHROME) / CELL);
            bankH = BANK_CHROME + rows * CELL;
            int total = bankH + GAP + INV_H;
            bankX = (width - BANK_W) / 2;
            bankY = Math.max(MARGIN, (height - total) / 2);
            leftPos = (width - INV_W) / 2;
            topPos = bankY + bankH + GAP;
        }
        tabY = bankY + 16;
        gridX = bankX + 8;
        gridY = bankY + 38;
        scrollX = bankX + 170;
        row1Y = gridY + rows * CELL + 6;
        row2Y = row1Y + 16;
    }

    private static int clampRows(int rows) {
        return Math.clamp(rows, MIN_ROWS, MAX_ROWS);
    }

    // --- Helpers ---

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
        return Math.max(0, (visible().size() + COLS - 1) / COLS - rows);
    }

    private boolean inGrid(double mx, double my) {
        return mx >= gridX && my >= gridY && mx < gridX + COLS * CELL && my < gridY + rows * CELL;
    }

    private boolean inBankPanel(double mx, double my) {
        return mx >= bankX && my >= bankY && mx < bankX + BANK_W && my < bankY + bankH;
    }

    private boolean inInventoryPanel(double mx, double my) {
        return mx >= leftPos && my >= topPos && mx < leftPos + INV_W && my < topPos + INV_H;
    }

    // Grid cell under the mouse -> bank entry index, or -1
    private int entryAt(double mx, double my) {
        if (!inGrid(mx, my)) return -1;
        int slot = (scrollRow + ((int) my - gridY) / CELL) * COLS + ((int) mx - gridX) / CELL;
        List<Integer> shown = visible();
        return slot < shown.size() ? shown.get(slot) : -1;
    }

    // Tab under the mouse: 0 = All, 1-9 = tabs, 10 = "+" (new tab), -1 = none
    private int tabAt(double mx, double my) {
        int tx = (int) mx - bankX - 4, ty = (int) my - tabY;
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

    private void askAmount(IntConsumer then) {
        pendingX = then;
        amountBox.setValue("");
        amountBox.setVisible(true);
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

        // Panels
        bevel(g, bankX, bankY, BANK_W, bankH, PANEL);
        bevel(g, leftPos, topPos, INV_W, INV_H, PANEL);

        // Title row
        if (!searching) g.centeredText(font, title, bankX + BANK_W / 2, bankY + 5, ORANGE);
        g.text(font, String.valueOf(ClientBank.entries.size()), bankX + 6, bankY + 5, YELLOW, true);

        // Tabs
        int shownTabs = highestTab() + 1;
        for (int t = 0; t <= Math.min(shownTabs, PlayerBank.MAX_TABS); t++) {
            int tx = bankX + 4 + t * TAB_W;
            boolean plus = t == shownTabs;
            bevel(g, tx, tabY, TAB_W, TAB_H, !plus && t == ClientBank.currentTab ? BUTTON_ON : BUTTON);
            if (plus) {
                g.centeredText(font, "+", tx + TAB_W / 2, tabY + 6, ORANGE);
            } else if (t == 0) {
                g.item(new ItemStack(Items.CHEST), tx, tabY + 2);
            } else {
                for (PlayerBank.Entry entry : ClientBank.entries) {
                    if (entry.tab() == t) { g.item(entry.item(), tx, tabY + 2); break; }
                }
            }
        }

        // Grid
        bevel(g, gridX - 2, gridY - 2, COLS * CELL + 4, rows * CELL + 4, PANEL_INNER);
        List<Integer> shown = visible();
        int hovered = entryAt(mouseX, mouseY);
        for (int i = 0; i < COLS * rows; i++) {
            int slot = scrollRow * COLS + i;
            if (slot >= shown.size()) break;
            int index = shown.get(slot);
            if (index == pressIndex && isDragging(mouseX, mouseY)) continue;
            int cx = gridX + (i % COLS) * CELL, cy = gridY + (i / COLS) * CELL;
            if (index == hovered) g.fill(cx, cy, cx + CELL, cy + CELL, 0x30FFFFFF);
            drawEntry(g, ClientBank.entries.get(index), cx + 2, cy + 3);
        }

        // Scrollbar
        int max = maxScroll();
        int trackH = rows * CELL;
        g.fill(scrollX, gridY, scrollX + 6, gridY + trackH, FRAME_DARK);
        int thumbH = Math.max(10, trackH * rows / (rows + max));
        int thumbY = max == 0 ? 0 : (trackH - thumbH) * scrollRow / max;
        g.fill(scrollX + 1, gridY + thumbY, scrollX + 5, gridY + thumbY + thumbH, FRAME_LIGHT);

        // Control rows
        g.text(font, Component.translatable("screen.skillcraft.bank_quantity"), bankX + 8, row2Y + 3, ORANGE, true);
        for (int i = 0; i < buttons.size(); i++) {
            Button b = buttons.get(i);
            boolean on = (i == 0 && !ClientBank.insertMode) || (i == 1 && ClientBank.insertMode) || (i == 2 && searching)
                    || (i >= 5 && i - 5 == ClientBank.quantityMode);
            bevel(g, b.x, b.y, b.w, b.h, on ? BUTTON_ON : BUTTON);
            String label = b.label != null ? Component.translatable(b.label).getString() : QUANTITY_LABELS[i - 5];
            g.centeredText(font, label, b.x + b.w / 2, b.y + 3, ORANGE);
        }

        // Inventory slot backgrounds
        for (Slot slot : menu.slots) {
            g.fill(leftPos + slot.x - 1, topPos + slot.y - 1, leftPos + slot.x + 17, topPos + slot.y + 17, PANEL_INNER);
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
        g.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, ORANGE, true);
    }

    private boolean isDragging(double mx, double my) {
        return pressIndex >= 0 && (Math.abs(mx - pressX) > 4 || Math.abs(my - pressY) > 4);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);

        // Dragged bank item follows the mouse
        if (isDragging(mouseX, mouseY) && pressIndex < ClientBank.entries.size()) {
            g.nextStratum();
            drawEntry(g, ClientBank.entries.get(pressIndex), mouseX - 8, mouseY - 8);
        }

        // "Enter amount" prompt
        if (amountBox.isVisible()) {
            g.nextStratum();
            int bx = bankX + BANK_W / 2 - 50;
            bevel(g, bx, bankY + 64, 100, 38, PANEL);
            g.centeredText(font, Component.translatable("screen.skillcraft.bank_enter_amount"), bankX + BANK_W / 2, bankY + 70, ORANGE);
            amountBox.extractRenderState(g, mouseX, mouseY, partialTick);
        }

        // Right-click "Choose Option" menu
        if (contextMenu != null) {
            g.nextStratum();
            int w = contextWidth();
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
        if (index >= 0 && menu.getCarried().isEmpty()) {
            PlayerBank.Entry entry = ClientBank.entries.get(index);
            List<Component> lines = new ArrayList<>(getTooltipFromContainerItem(entry.item()));
            lines.add(Component.translatable("screen.skillcraft.bank_amount", new DecimalFormat("#,###").format(entry.count()))
                    .withStyle(ChatFormatting.YELLOW));
            g.setTooltipForNextFrame(font, lines, entry.item().getTooltipImage(), entry.item(), mouseX, mouseY);
            return;
        }
        super.extractTooltip(g, mouseX, mouseY);
    }

    // --- Context menus ---

    private Component nameOf(ItemStack stack) {
        return stack.getHoverName().copy().withStyle(ChatFormatting.GOLD);
    }

    private int contextWidth() {
        int w = font.width(contextTitle) + 8;
        for (MenuOption option : contextMenu) w = Math.max(w, font.width(option.label()) + 8);
        return w;
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
            options.add(new MenuOption(Component.translatable("screen.skillcraft.bank_deposit_n", amount), () -> send(BankActions.Action.DEPOSIT, slotIndex, amount, ClientBank.currentTab)));
        }
        options.add(new MenuOption(Component.translatable("screen.skillcraft.bank_deposit_x"), () -> askAmount(amount -> send(BankActions.Action.DEPOSIT, slotIndex, amount, ClientBank.currentTab))));
        options.add(new MenuOption(Component.translatable("screen.skillcraft.bank_deposit_all"), () -> send(BankActions.Action.DEPOSIT, slotIndex, BankActions.ALL, ClientBank.currentTab)));
        openMenu(options, nameOf(stack), mx, my);
    }

    private void openMenu(List<MenuOption> options, Component title, double mx, double my) {
        contextMenu = options;
        contextTitle = title;
        int w = contextWidth(), h = 12 + options.size() * 12;
        // Keep the menu on screen
        contextX = Math.clamp((int) mx - 10, 2, Math.max(2, width - w - 2));
        contextY = Math.clamp((int) my - 4, 2, Math.max(2, height - h - 2));
    }

    // --- Input ---

    @Override
    protected boolean hasClickedOutside(double mx, double my, int xo, int yo) {
        return super.hasClickedOutside(mx, my, xo, yo) && !inBankPanel(mx, my);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x(), my = event.y();
        boolean right = event.input() == InputConstants.MOUSE_BUTTON_RIGHT;

        if (contextMenu != null) {
            int w = contextWidth();
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
            amountBox.mouseClicked(event, doubleClick);
            return true;
        }
        if (searching && search.mouseClicked(event, doubleClick)) return true;

        if (inBankPanel(mx, my)) {
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
            // Holding an item: drop it into the bank (right-click: just one)
            if (!menu.getCarried().isEmpty()) {
                if (inGrid(mx, my)) send(BankActions.Action.DEPOSIT_CARRIED, 0, right ? 1 : BankActions.ALL, ClientBank.currentTab);
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
            }
            return true;
        }

        // Shift-click an inventory item to deposit it
        Slot slot = getHoveredSlot();
        if (slot != null && slot.hasItem() && event.hasShiftDown() && menu.getCarried().isEmpty()) {
            if (right) {
                openDepositMenu(slot.index, slot.getItem(), mx, my);
            } else {
                send(BankActions.Action.DEPOSIT, slot.index, currentQuantity(), ClientBank.currentTab);
            }
            return true;
        }
        // Everything else in the inventory works like a normal inventory
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (pressIndex < 0) return super.mouseReleased(event);
        double mx = event.x(), my = event.y();
        int from = pressIndex;
        boolean dragged = isDragging(mx, my);
        pressIndex = -1;
        if (!dragged || inInventoryPanel(mx, my)) {
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
        if (inBankPanel(mx, my)) {
            scrollRow = Math.clamp(scrollRow - (int) Math.signum(scrollY), 0, maxScroll());
            return true;
        }
        return super.mouseScrolled(mx, my, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (amountBox.isVisible()) {
            if (event.key() == InputConstants.KEY_RETURN || event.key() == InputConstants.KEY_NUMPADENTER) {
                confirmAmount();
            } else if (event.isEscape()) {
                amountBox.setVisible(false);
                pendingX = null;
            } else {
                amountBox.keyPressed(event);
            }
            return true;
        }
        if (contextMenu != null && event.isEscape()) {
            contextMenu = null;
            return true;
        }
        if (searching && search.isFocused()) {
            if (event.isEscape()) toggleSearch();
            else search.keyPressed(event);
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
