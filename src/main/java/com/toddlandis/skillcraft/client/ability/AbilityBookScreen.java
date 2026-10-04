package com.toddlandis.skillcraft.client.ability;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.mojang.blaze3d.platform.InputConstants;
import com.toddlandis.skillcraft.ability.AbilityCategory;
import com.toddlandis.skillcraft.ability.AbilityClass;
import com.toddlandis.skillcraft.ability.AbilityData;
import com.toddlandis.skillcraft.ability.AbilityDefinition;
import com.toddlandis.skillcraft.ability.AbilityService;
import com.toddlandis.skillcraft.network.SetLoadoutPayload;
import com.toddlandis.skillcraft.skill.SkillManager;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

// The Ability Book (default key J, spec 2.5). Left: tabs, search, a "usable with my weapon" filter
// and ability cards. Right: the 3 saved bars. Drag a card onto a slot, or click a slot and then a
// card (clicking a card with no slot selected fills the first empty slot); right-click a slot to
// clear it. Like the bank screen, the two panels sit side by side when there's room and stack
// otherwise, so it fits every GUI scale. Changes are sent to the server, which can refuse them
// (e.g. switching the active bar in combat); the book then snaps back to what the server has.
public class AbilityBookScreen extends Screen {
    private static final int MARGIN = 6, GAP = 6, PAD = 6;
    private static final int CARD_W = 120, CARD_H = 30, CARD_GAP = 4;
    private static final int TAB_H = 13;
    private static final int BAR_W = 130, BAR_H = 102, BIG_SLOT = 22;

    private static final int PANEL = 0xF0181820, PANEL_EDGE = 0xFF3A3A48, CARD = 0xFF262630, CARD_HOVER = 0xFF33333F, CARD_LOCKED = 0xFF1C1C22;
    private static final int GOLD = 0xFFFFD34A, GRAY = 0xFF9A9AA6, WHITE = 0xFFFFFFFF, RED = 0xFFE06060, GREEN = 0xFF7EDB6A;

    private AbilityCategory tab = AbilityCategory.MELEE;
    private boolean weaponFilter;
    private int scroll;
    private int editing;
    private int selectedSlot = -1;
    private final Identifier[][] bars = new Identifier[AbilityData.LOADOUTS][AbilityData.SLOTS];
    private final String[] names = new String[AbilityData.LOADOUTS];
    private long pendingUntil;

    // Drag and drop
    private Identifier pressed;
    private double pressX, pressY;
    private boolean dragging;

    // Layout (screen coordinates), recomputed in init()
    private int catX, catY, catW, catH, columns, rows, gridX, gridY;
    private int barX, barY;
    private final List<int[]> tabRects = new ArrayList<>(); // x, y, w, ordinal
    private int filterX, filterY, filterW;
    private EditBox search, nameBox;

    public AbilityBookScreen() {
        super(Component.translatable("screen.skillcraft.ability_book"));
    }

    @Override
    protected void init() {
        super.init();
        if (minecraft.player != null && pendingUntil == 0) {
            AbilityData data = ClientAbilities.data(minecraft.player);
            editing = data.activeLoadout();
            copyFrom(data);
        }
        layout();
        String previous = search == null ? "" : search.getValue();
        search = new EditBox(font, gridX, catY + 18 + tabRows() * (TAB_H + 2), Math.max(60, catW - PAD * 2 - filterW - 6), 12,
                Component.translatable("screen.skillcraft.ability_search"));
        search.setMaxLength(32);
        search.setValue(previous);
        search.setHint(Component.translatable("screen.skillcraft.ability_search").withStyle(ChatFormatting.DARK_GRAY));
        search.setResponder(text -> scroll = 0);
        addRenderableWidget(search);

        nameBox = new EditBox(font, barX + PAD, barY + 30, BAR_W - PAD * 2, 12, Component.translatable("screen.skillcraft.ability_bar_name"));
        nameBox.setMaxLength(AbilityData.NAME_MAX);
        nameBox.setValue(names[editing]);
        nameBox.setHint(Component.translatable("screen.skillcraft.ability_bar_default", editing + 1).withStyle(ChatFormatting.DARK_GRAY));
        nameBox.setResponder(text -> names[editing] = text);
        addRenderableWidget(nameBox);
    }

    private void copyFrom(AbilityData data) {
        for (int l = 0; l < AbilityData.LOADOUTS; l++) {
            bars[l] = data.bar(l);
            names[l] = data.name(l);
        }
    }

    // --- Layout ---

    private void layout() {
        int full = width - MARGIN * 2;
        // Side by side whenever one column of cards fits next to the bar panel (stacked only on very narrow screens)
        boolean sideBySide = full >= cardsWidth(1) + GAP + BAR_W;
        int available = sideBySide ? full - GAP - BAR_W : full;
        columns = Math.clamp((available - PAD * 2 + CARD_GAP) / (CARD_W + CARD_GAP), 1, 3);
        catW = cardsWidth(columns);
        filterW = font.width(Component.translatable("screen.skillcraft.ability_filter")) + 14;
        computeTabs();
        int header = 18 + tabRows() * (TAB_H + 2) + 16;
        if (sideBySide) {
            int total = catW + GAP + BAR_W;
            catX = (width - total) / 2;
            barX = catX + catW + GAP;
            int maxRows = Math.max(1, (height - MARGIN * 2 - header - PAD) / (CARD_H + CARD_GAP));
            rows = Math.min(maxRows, 6);
            catH = header + rows * (CARD_H + CARD_GAP) + PAD;
            catY = Math.max(MARGIN, (height - Math.max(catH, BAR_H)) / 2);
            barY = catY;
        } else {
            catX = (width - catW) / 2;
            barX = (width - BAR_W) / 2;
            barY = MARGIN;
            catY = barY + BAR_H + GAP;
            int maxRows = Math.max(1, (height - catY - MARGIN - header - PAD) / (CARD_H + CARD_GAP));
            rows = Math.min(maxRows, 6);
            catH = header + rows * (CARD_H + CARD_GAP) + PAD;
        }
        gridX = catX + PAD;
        gridY = catY + header;
        filterX = catX + catW - PAD - filterW;
        filterY = catY + 18 + tabRows() * (TAB_H + 2);
        computeTabs();
    }

    private static int cardsWidth(int cols) {
        return cols * CARD_W + (cols - 1) * CARD_GAP + PAD * 2;
    }

    // Tabs wrap onto a second row when the panel is narrow
    private void computeTabs() {
        tabRects.clear();
        int x = catX + PAD, y = catY + 16, right = catX + catW - PAD;
        for (AbilityCategory category : AbilityCategory.values()) {
            int w = font.width(category.displayName()) + 10;
            if (x + w > right && x > catX + PAD) {
                x = catX + PAD;
                y += TAB_H + 2;
            }
            tabRects.add(new int[]{x, y, w, category.ordinal()});
            x += w + 2;
        }
    }

    private int tabRows() {
        if (tabRects.isEmpty()) return 1;
        int first = tabRects.getFirst()[1], last = tabRects.getLast()[1];
        return (last - first) / (TAB_H + 2) + 1;
    }

    // --- Data ---

    private List<Map.Entry<Identifier, AbilityDefinition>> visible() {
        List<Map.Entry<Identifier, AbilityDefinition>> result = new ArrayList<>();
        Player player = minecraft.player;
        String filter = search == null ? "" : search.getValue().toLowerCase(Locale.ROOT).trim();
        for (Map.Entry<Identifier, AbilityDefinition> entry : ClientAbilities.definitions.entrySet()) {
            AbilityDefinition def = entry.getValue();
            if (def.category() != tab) continue;
            if (!filter.isEmpty() && !AbilityDefinition.name(entry.getKey()).getString().toLowerCase(Locale.ROOT).contains(filter)) continue;
            if (weaponFilter && player != null && !def.requires().weaponMatches(player.getMainHandItem(), player.getOffhandItem())) continue;
            result.add(entry);
        }
        return result;
    }

    private int maxScroll() {
        return Math.max(0, (visible().size() + columns - 1) / columns - rows);
    }

    public static SetLoadoutPayload payload(AbilityData data, int loadout, boolean activate) {
        return payload(data.bar(loadout), data.name(loadout), loadout, activate);
    }

    private static SetLoadoutPayload payload(Identifier[] bar, String name, int loadout, boolean activate) {
        List<String> slots = new ArrayList<>();
        for (Identifier id : bar) slots.add(id == null ? "" : id.toString());
        return new SetLoadoutPayload(loadout, slots, name == null ? "" : name, activate);
    }

    private void send(boolean activate) {
        ClientPacketDistributor.sendToServer(payload(bars[editing], names[editing], editing, activate));
        pendingUntil = ClientAbilities.now() + 20;
    }

    private void assign(int slot, Identifier id) {
        Identifier[] bar = bars[editing];
        for (int s = 0; s < bar.length; s++) if (id != null && id.equals(bar[s])) bar[s] = null; // move, don't duplicate
        bar[slot] = id;
        send(false);
    }

    @Override
    public void tick() {
        super.tick();
        // After the server answers (or refuses), show what it actually has
        if (minecraft.player != null && ClientAbilities.now() > pendingUntil) {
            AbilityData data = ClientAbilities.data(minecraft.player);
            for (int l = 0; l < AbilityData.LOADOUTS; l++) bars[l] = data.bar(l);
        }
        scroll = Math.clamp(scroll, 0, maxScroll());
    }

    // --- Drawing ---

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        Player player = minecraft.player;
        if (player == null) return;
        AbilityData data = ClientAbilities.data(player);

        // Catalogue panel
        panel(g, catX, catY, catW, catH);
        g.text(font, title, catX + PAD, catY + 5, GOLD, true);
        for (int[] rect : tabRects) {
            AbilityCategory category = AbilityCategory.values()[rect[3]];
            boolean selected = category == tab;
            int color = selected ? category.color() : 0xFF2C2C36;
            g.fill(rect[0], rect[1], rect[0] + rect[2], rect[1] + TAB_H, color);
            if (!selected) g.fill(rect[0], rect[1] + TAB_H - 2, rect[0] + rect[2], rect[1] + TAB_H, category.color());
            g.text(font, category.displayName(), rect[0] + 5, rect[1] + 3, category.comingLater() ? GRAY : WHITE, selected);
        }
        // Filter toggle
        g.fill(filterX, filterY, filterX + filterW, filterY + 12, weaponFilter ? 0xFF3E6A3A : 0xFF2C2C36);
        g.fill(filterX + 2, filterY + 3, filterX + 8, filterY + 9, weaponFilter ? GREEN : 0xFF55555F);
        g.text(font, Component.translatable("screen.skillcraft.ability_filter"), filterX + 11, filterY + 2, WHITE, false);

        Map.Entry<Identifier, AbilityDefinition> hovered = null;
        if (tab.comingLater()) {
            g.centeredText(font, Component.translatable("screen.skillcraft.ability_coming." + tab.getSerializedName()), catX + catW / 2, gridY + 20, GRAY);
        } else {
            List<Map.Entry<Identifier, AbilityDefinition>> shown = visible();
            if (shown.isEmpty()) g.centeredText(font, Component.translatable("screen.skillcraft.ability_none"), catX + catW / 2, gridY + 20, GRAY);
            for (int i = 0; i < columns * rows; i++) {
                int index = scroll * columns + i;
                if (index >= shown.size()) break;
                Map.Entry<Identifier, AbilityDefinition> entry = shown.get(index);
                int cx = gridX + (i % columns) * (CARD_W + CARD_GAP), cy = gridY + (i / columns) * (CARD_H + CARD_GAP);
                boolean over = mouseX >= cx && mouseX < cx + CARD_W && mouseY >= cy && mouseY < cy + CARD_H;
                if (over) hovered = entry;
                drawCard(g, player, entry.getKey(), entry.getValue(), cx, cy, over);
            }
            // Scrollbar
            int max = maxScroll();
            if (max > 0) {
                int trackH = rows * (CARD_H + CARD_GAP) - CARD_GAP;
                int knobH = Math.max(10, trackH * rows / (rows + max));
                int knobY = gridY + (trackH - knobH) * scroll / max;
                g.fill(catX + catW - 4, gridY, catX + catW - 2, gridY + trackH, 0xFF101014);
                g.fill(catX + catW - 4, knobY, catX + catW - 2, knobY + knobH, 0xFF6A6A78);
            }
        }

        // Bar panel
        panel(g, barX, barY, BAR_W, BAR_H);
        g.text(font, Component.translatable("screen.skillcraft.ability_bar"), barX + PAD, barY + 5, GOLD, true);
        for (int l = 0; l < AbilityData.LOADOUTS; l++) {
            int bx = barX + PAD + l * 20, by = barY + 15;
            boolean active = l == data.activeLoadout();
            g.fill(bx, by, bx + 18, by + 12, l == editing ? 0xFF4A4A5C : 0xFF2C2C36);
            if (active) g.outline(bx, by, 18, 12, GOLD);
            g.centeredText(font, String.valueOf(l + 1), bx + 9, by + 2, active ? GOLD : WHITE);
        }
        if (editing != data.activeLoadout()) {
            int ux = barX + PAD + 64, uy = barY + 15;
            g.fill(ux, uy, barX + BAR_W - PAD, uy + 12, 0xFF3E6A3A);
            g.centeredText(font, Component.translatable("screen.skillcraft.ability_use_bar"), (ux + barX + BAR_W - PAD) / 2, uy + 2, WHITE);
        } else {
            g.text(font, Component.translatable("screen.skillcraft.ability_active"), barX + PAD + 64, barY + 17, GREEN, false);
        }
        int slotsX = barX + (BAR_W - (AbilityData.SLOTS * BIG_SLOT + (AbilityData.SLOTS - 1) * 4)) / 2;
        int slotsY = barY + 48;
        for (int s = 0; s < AbilityData.SLOTS; s++) {
            int sx = slotsX + s * (BIG_SLOT + 4);
            AbilityHud.drawSlot(g, font, player, bars[editing][s], AbilityKeys.label(s), sx + 1, slotsY + 1);
            if (s == selectedSlot) g.outline(sx, slotsY, BIG_SLOT, BIG_SLOT, GOLD);
            if (mouseX >= sx && mouseX < sx + BIG_SLOT && mouseY >= slotsY && mouseY < slotsY + BIG_SLOT && bars[editing][s] != null && !dragging) {
                AbilityDefinition def = ClientAbilities.get(bars[editing][s]);
                if (def != null) hovered = Map.entry(bars[editing][s], def);
            }
        }
        g.text(font, Component.translatable("screen.skillcraft.ability_momentum", Math.round(data.momentum())), barX + PAD, barY + 74, GRAY, false);
        g.textWithWordWrap(font, Component.translatable("screen.skillcraft.ability_hint"), barX + PAD, barY + 83, BAR_W - PAD * 2, 0xFF6A6A78, false);

        // Dragged card follows the mouse
        if (dragging && pressed != null) {
            AbilityDefinition def = ClientAbilities.get(pressed);
            if (def != null) g.blitSprite(RenderPipelines.GUI_TEXTURED, def.icon(), mouseX - 8, mouseY - 8, 16, 16);
        } else if (hovered != null) {
            g.setComponentTooltipForNextFrame(font, tooltip(player, hovered.getKey(), hovered.getValue()), mouseX, mouseY);
        }
    }

    private void panel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, PANEL);
        g.outline(x, y, w, h, PANEL_EDGE);
    }

    private void drawCard(GuiGraphicsExtractor g, Player player, Identifier id, AbilityDefinition def, int x, int y, boolean over) {
        boolean unlocked = ClientAbilities.unlocked(player, id, def);
        g.fill(x, y, x + CARD_W, y + CARD_H, unlocked ? (over ? CARD_HOVER : CARD) : CARD_LOCKED);
        g.fill(x, y, x + 2, y + CARD_H, def.category().color());
        g.fill(x + 4, y + 6, x + 22, y + 24, 0xFF000000);
        g.blitSprite(RenderPipelines.GUI_TEXTURED, def.icon(), x + 5, y + 7, 16, 16, unlocked ? 0xFFFFFFFF : 0xFF4A4A4A);
        Component name = AbilityDefinition.name(id);
        g.text(font, font.substrByWidth(name, CARD_W - 30).getString(), x + 26, y + 5, unlocked ? WHITE : GRAY, false);
        Component info = unlocked
                ? Component.translatable("screen.skillcraft.ability_card", def.abilityClass().displayName(), seconds(def.cooldown()))
                : lockText(player, id, def);
        g.text(font, font.substrByWidth(info, CARD_W - 30).getString(), x + 26, y + 17, unlocked ? classColor(def.abilityClass()) : RED, false);
        if (!unlocked) {
            // a small padlock in the corner
            int lx = x + CARD_W - 9, ly = y + 3;
            g.fill(lx + 1, ly, lx + 5, ly + 1, GRAY);
            g.fill(lx, ly + 1, lx + 1, ly + 3, GRAY);
            g.fill(lx + 5, ly + 1, lx + 6, ly + 3, GRAY);
            g.fill(lx - 1, ly + 3, lx + 7, ly + 8, GRAY);
        }
    }

    private Component lockText(Player player, Identifier id, AbilityDefinition def) {
        AbilityDefinition.Requirement req = def.requires();
        if (SkillManager.level(player, req.skill()) < req.level()) {
            return Component.translatable("screen.skillcraft.ability_needs_level", req.skill().displayName(), req.level());
        }
        return Component.translatable("screen.skillcraft.ability_needs_codex");
    }

    private static int classColor(AbilityClass abilityClass) {
        return switch (abilityClass) {
            case BASIC -> 0xFFB8B8C4;
            case SURGE -> 0xFF6FB6FF;
            case ULTIMATE -> 0xFFFFC14A;
        };
    }

    private static String seconds(int ticks) {
        return ticks % 20 == 0 ? String.valueOf(ticks / 20) : String.format("%.1f", ticks / 20.0);
    }

    private List<Component> tooltip(Player player, Identifier id, AbilityDefinition def) {
        List<Component> lines = new ArrayList<>();
        AbilityDefinition.Requirement req = def.requires();
        lines.add(AbilityDefinition.name(id).copy().withStyle(ChatFormatting.GOLD));
        lines.add(Component.translatable("screen.skillcraft.ability_class_line", def.abilityClass().displayName(), def.category().displayName()).withStyle(ChatFormatting.GRAY));
        boolean levelOk = player.isCreative() || SkillManager.level(player, req.skill()) >= req.level();
        lines.add(Component.translatable("screen.skillcraft.ability_requires", req.skill().displayName(), req.level())
                .withStyle(levelOk ? ChatFormatting.GREEN : ChatFormatting.RED));
        if (req.weapon().isPresent() || req.offhand().isPresent() || req.weaponStyle().isPresent()) {
            boolean ok = req.weaponMatches(player.getMainHandItem(), player.getOffhandItem());
            Component weapon = req.weaponStyle().map(style -> (Component) Component.translatable("screen.skillcraft.ability_weapon_styled",
                    AbilityService.weaponName(def), style.displayName())).orElse(AbilityService.weaponName(def));
            lines.add(Component.translatable("screen.skillcraft.ability_weapon", weapon)
                    .withStyle(ok ? ChatFormatting.GREEN : ChatFormatting.RED));
        }
        if (req.codex()) {
            boolean learned = ClientAbilities.data(player).hasLearned(id);
            lines.add(Component.translatable(learned ? "screen.skillcraft.ability_codex_learned" : "screen.skillcraft.ability_codex")
                    .withStyle(learned ? ChatFormatting.GREEN : ChatFormatting.RED));
        }
        lines.add(Component.translatable("screen.skillcraft.ability_cooldown", seconds(def.cooldown())).withStyle(ChatFormatting.GRAY));
        if (def.windup() > 0) lines.add(Component.translatable("screen.skillcraft.ability_windup", seconds(def.windup())).withStyle(ChatFormatting.GRAY));
        if (def.offGcd()) lines.add(Component.translatable("screen.skillcraft.ability_off_gcd").withStyle(ChatFormatting.AQUA));
        int level = AbilityService.skillLevel(player, def);
        if (def.momentum().generate() > 0) lines.add(Component.translatable("screen.skillcraft.ability_generates", def.momentum().generate()).withStyle(ChatFormatting.GOLD));
        if (def.momentumRequired(level) > 0) {
            lines.add(Component.translatable(def.abilityClass() == AbilityClass.ULTIMATE ? "screen.skillcraft.ability_momentum_all" : "screen.skillcraft.ability_momentum_cost",
                    Math.round(def.momentumRequired(level)), Math.round(def.momentumCost(level))).withStyle(ChatFormatting.GOLD));
        }
        def.toggle().ifPresent(toggle -> lines.add(Component.translatable("screen.skillcraft.ability_toggle", String.format("%.1f", toggle.drainPerSecond())).withStyle(ChatFormatting.GOLD)));
        for (AbilityDefinition.ItemCost cost : def.itemCosts()) {
            lines.add(Component.translatable("screen.skillcraft.ability_item_cost", cost.count(), cost.item().getDefaultInstance().getHoverName()).withStyle(ChatFormatting.GOLD));
        }
        for (Component detail : def.behavior().details()) lines.add(detail.copy().withStyle(ChatFormatting.GRAY));
        lines.add(AbilityDefinition.description(id).copy().withStyle(ChatFormatting.WHITE));
        return lines;
    }

    // --- Input ---

    private Identifier cardAt(double mx, double my) {
        if (tab.comingLater() || mx < gridX || my < gridY) return null;
        int col = (int) (mx - gridX) / (CARD_W + CARD_GAP), row = (int) (my - gridY) / (CARD_H + CARD_GAP);
        if (col >= columns || row >= rows) return null;
        if ((mx - gridX) % (CARD_W + CARD_GAP) >= CARD_W || (my - gridY) % (CARD_H + CARD_GAP) >= CARD_H) return null;
        int index = (scroll + row) * columns + col;
        List<Map.Entry<Identifier, AbilityDefinition>> shown = visible();
        return index < shown.size() ? shown.get(index).getKey() : null;
    }

    private int slotAt(double mx, double my) {
        int slotsX = barX + (BAR_W - (AbilityData.SLOTS * BIG_SLOT + (AbilityData.SLOTS - 1) * 4)) / 2;
        int slotsY = barY + 48;
        if (my < slotsY || my >= slotsY + BIG_SLOT) return -1;
        for (int s = 0; s < AbilityData.SLOTS; s++) {
            int sx = slotsX + s * (BIG_SLOT + 4);
            if (mx >= sx && mx < sx + BIG_SLOT) return s;
        }
        return -1;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x(), my = event.y();
        boolean right = event.input() == InputConstants.MOUSE_BUTTON_RIGHT;
        Player player = minecraft.player;
        if (player == null) return super.mouseClicked(event, doubleClick);

        for (int[] rect : tabRects) {
            if (mx >= rect[0] && mx < rect[0] + rect[2] && my >= rect[1] && my < rect[1] + TAB_H) {
                tab = AbilityCategory.values()[rect[3]];
                scroll = 0;
                return true;
            }
        }
        if (mx >= filterX && mx < filterX + filterW && my >= filterY && my < filterY + 12) {
            weaponFilter = !weaponFilter;
            scroll = 0;
            return true;
        }
        for (int l = 0; l < AbilityData.LOADOUTS; l++) {
            int bx = barX + PAD + l * 20, by = barY + 15;
            if (mx >= bx && mx < bx + 18 && my >= by && my < by + 12) {
                editing = l;
                selectedSlot = -1;
                nameBox.setValue(names[l]);
                nameBox.setHint(Component.translatable("screen.skillcraft.ability_bar_default", l + 1).withStyle(ChatFormatting.DARK_GRAY));
                return true;
            }
        }
        AbilityData data = ClientAbilities.data(player);
        if (editing != data.activeLoadout() && mx >= barX + PAD + 64 && mx < barX + BAR_W - PAD && my >= barY + 15 && my < barY + 27) {
            send(true);
            return true;
        }
        int slot = slotAt(mx, my);
        if (slot >= 0) {
            if (right) {
                assign(slot, null);
                selectedSlot = -1;
            } else {
                selectedSlot = selectedSlot == slot ? -1 : slot;
            }
            return true;
        }
        Identifier card = cardAt(mx, my);
        if (card != null && !right) {
            AbilityDefinition def = ClientAbilities.get(card);
            if (def != null && ClientAbilities.unlocked(player, card, def)) {
                pressed = card;
                pressX = mx;
                pressY = my;
                dragging = false;
            }
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (pressed != null && !dragging && Math.abs(event.x() - pressX) + Math.abs(event.y() - pressY) > 4) dragging = true;
        return pressed != null || super.mouseDragged(event, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (pressed == null) return super.mouseReleased(event);
        Identifier card = pressed;
        boolean wasDragging = dragging;
        pressed = null;
        dragging = false;
        int slot = slotAt(event.x(), event.y());
        if (wasDragging) {
            if (slot >= 0) assign(slot, card);
            return true;
        }
        // A plain click: into the selected slot, or the first empty one
        int target = selectedSlot;
        if (target < 0) {
            for (int s = 0; s < AbilityData.SLOTS; s++) {
                if (bars[editing][s] == null) {
                    target = s;
                    break;
                }
            }
        }
        if (target >= 0) assign(target, card);
        selectedSlot = -1;
        return true;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        if (mx >= catX && mx < catX + catW && my >= gridY && my < catY + catH) {
            scroll = Math.clamp(scroll - (int) Math.signum(scrollY), 0, maxScroll());
            return true;
        }
        return super.mouseScrolled(mx, my, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if ((search.isFocused() || nameBox.isFocused()) && !event.isEscape()) {
            if (nameBox.isFocused() && (event.key() == InputConstants.KEY_RETURN || event.key() == InputConstants.KEY_NUMPADENTER)) {
                nameBox.setFocused(false);
                send(false);
                return true;
            }
            return (search.isFocused() ? search : nameBox).keyPressed(event) || true;
        }
        if (AbilityKeys.BOOK.matches(event)) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (search.isFocused()) return search.charTyped(event);
        if (nameBox.isFocused()) return nameBox.charTyped(event);
        return super.charTyped(event);
    }

    @Override
    public void onClose() {
        // Save a renamed bar
        if (minecraft.player != null && !names[editing].equals(ClientAbilities.data(minecraft.player).name(editing))) send(false);
        super.onClose();
    }

    // For the dev showcase (dev/Showcase): open on a given tab
    public void showTab(AbilityCategory category) {
        tab = category;
        scroll = 0;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
