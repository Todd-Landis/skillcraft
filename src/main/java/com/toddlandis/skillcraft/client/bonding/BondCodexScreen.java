package com.toddlandis.skillcraft.client.bonding;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import com.toddlandis.skillcraft.Config;
import com.toddlandis.skillcraft.bonding.BondCodex;
import com.toddlandis.skillcraft.bonding.BondService;
import com.toddlandis.skillcraft.bonding.Bondable;
import com.toddlandis.skillcraft.client.ClientEvents;
import com.toddlandis.skillcraft.network.CodexActionPayload;
import com.toddlandis.skillcraft.registry.ModBlocks;
import com.toddlandis.skillcraft.registry.ModDataMaps;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

// The Bond Codex screen (key H, or use the codex in the air). Left: your bonds, each with its
// state and the buttons that apply to it. Right: the codex pages, every bondable species, named
// once you've bonded one and "???" until then. Buttons only ask; the server decides (BondService).
public class BondCodexScreen extends Screen {
    private static final int PANEL_W = 330;
    private static final int PANEL_H = 214;
    private static final int LIST_W = 214;
    private static final int ROW_H = 26;
    private static final int ROWS = 6;
    private static final int BTN_W = 46;
    private static final int BTN_H = 12;

    private record Button(int x, int y, int w, int h, CodexActionPayload.Action action, UUID bond, boolean enabled) {
        boolean contains(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    private final List<Button> buttons = new ArrayList<>();
    private int scroll;

    public BondCodexScreen() {
        super(Component.translatable("screen.skillcraft.bond_codex"));
    }

    private BondCodex codex() {
        return BondService.codex(minecraft.player);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        if (minecraft.player == null) return;
        BondCodex codex = codex();
        int level = BondService.level(minecraft.player);
        int left = (width - PANEL_W) / 2;
        int top = (height - PANEL_H) / 2;
        g.fill(left, top, left + PANEL_W, top + PANEL_H, 0xE0101418);
        g.text(font, title, left + 8, top + 7, 0xFFFFD27A, true);
        g.text(font, Component.translatable("screen.skillcraft.bond_summary", level, codex.bonds().size(), BondService.slots(level)),
                left + 8, top + 19, 0xFFAAAAAA, false);

        buttons.clear();
        boolean nearShrine = nearShrine();
        List<BondCodex.Bond> bonds = codex.bonds();
        scroll = Math.clamp(scroll, 0, Math.max(0, bonds.size() - ROWS));
        int y = top + 34;
        if (bonds.isEmpty()) {
            g.text(font, Component.translatable("screen.skillcraft.bond_empty"), left + 10, y + 4, 0xFF888888, false);
        }
        for (int i = scroll; i < Math.min(bonds.size(), scroll + ROWS); i++) {
            drawBond(g, bonds.get(i), left + 6, y, nearShrine, mouseX, mouseY);
            y += ROW_H + 2;
        }
        if (bonds.size() > ROWS) {
            g.text(font, Component.literal((scroll + 1) + "-" + Math.min(bonds.size(), scroll + ROWS) + " / " + bonds.size()),
                    left + 8, top + PANEL_H - 24, 0xFF777777, false);
        }

        drawPages(g, codex, left + LIST_W + 12, top + 34);
        g.text(font, Component.translatable("screen.skillcraft.bond_hint"), left + 8, top + PANEL_H - 12, 0xFF777777, false);
    }

    private void drawBond(GuiGraphicsExtractor g, BondCodex.Bond bond, int x, int y, boolean nearShrine, int mouseX, int mouseY) {
        g.fill(x, y, x + LIST_W, y + ROW_H, 0xFF1E242C);
        g.text(font, BondService.displayName(bond), x + 4, y + 3, 0xFFFFFFFF, true);
        Bondable data = BondService.bondable(bond.species);
        if (data != null) {
            Component kind = Component.translatable("screen.skillcraft.bond_kind." + data.kind().getSerializedName(), data.level());
            g.text(font, kind, x + 4, y + 14, 0xFF8899AA, false);
        }

        // State, and a health bar for living bonds
        int stateColor = switch (bond.state) {
            case STORED -> 0xFF88CC88;
            case OUT -> 0xFF66CCFF;
            case FALLEN -> 0xFFFF6666;
            case REKINDLING -> 0xFFFFB050;
        };
        Component state = bond.state == BondCodex.State.REKINDLING
                ? Component.translatable("screen.skillcraft.bond_state.rekindling_time", secondsLeft(bond))
                : Component.translatable("screen.skillcraft.bond_state." + bond.state.getSerializedName());
        g.text(font, state, x + 96, y + 3, stateColor, false);
        if (bond.maxHealth > 0 && bond.state != BondCodex.State.FALLEN && bond.state != BondCodex.State.REKINDLING) {
            int barW = 60;
            g.fill(x + 96, y + 15, x + 96 + barW, y + 18, 0xFF000000);
            g.fill(x + 96, y + 15, x + 96 + (int) (barW * Math.clamp(bond.health / bond.maxHealth, 0f, 1f)), y + 18, 0xFFCC3333);
        }

        int bx = x + LIST_W - BTN_W - 4;
        switch (bond.state) {
            case STORED -> {
                button(g, bx, y + 1, CodexActionPayload.Action.SUMMON, bond.id, true, mouseX, mouseY);
                button(g, bx, y + 13, CodexActionPayload.Action.RELEASE, bond.id, true, mouseX, mouseY);
            }
            case OUT -> button(g, bx, y + 7, CodexActionPayload.Action.STORE, bond.id, true, mouseX, mouseY);
            case FALLEN -> button(g, bx, y + 7, CodexActionPayload.Action.REKINDLE, bond.id, nearShrine, mouseX, mouseY);
            case REKINDLING -> {}
        }
    }

    private void button(GuiGraphicsExtractor g, int x, int y, CodexActionPayload.Action action, UUID bond, boolean enabled, int mouseX, int mouseY) {
        Button b = new Button(x, y, BTN_W, BTN_H, action, bond, enabled);
        buttons.add(b);
        boolean hover = enabled && b.contains(mouseX, mouseY);
        g.fill(x, y, x + BTN_W, y + BTN_H, !enabled ? 0xFF2A2A2A : hover ? 0xFF4C6A8C : 0xFF34485E);
        Component label = Component.translatable("screen.skillcraft.bond_action." + action.name().toLowerCase(java.util.Locale.ROOT));
        g.centeredText(font, label, x + BTN_W / 2, y + 2, enabled ? 0xFFFFFFFF : 0xFF777777);
    }

    // Every bondable species, by level: named once bonded, "???" until then
    private void drawPages(GuiGraphicsExtractor g, BondCodex codex, int x, int y) {
        List<Holder.Reference<EntityType<?>>> species = BuiltInRegistries.ENTITY_TYPE.listElements()
                .filter(h -> h.getData(ModDataMaps.BONDABLE) != null)
                .sorted(Comparator.comparingInt(h -> h.getData(ModDataMaps.BONDABLE).level()))
                .toList();
        long filled = species.stream().filter(h -> codex.pages().contains(BuiltInRegistries.ENTITY_TYPE.getKey(h.value()))).count();
        g.text(font, Component.translatable("screen.skillcraft.bond_pages", filled, species.size()), x, y - 12, 0xFFFFD27A, false);
        int level = BondService.level(minecraft.player);
        for (Holder.Reference<EntityType<?>> holder : species) {
            if (y > height / 2 + PANEL_H / 2 - 26) break;
            Bondable data = holder.getData(ModDataMaps.BONDABLE);
            Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(holder.value());
            boolean known = codex.pages().contains(id);
            Component name = known ? data.summonType(holder.value()).getDescription() : Component.literal("???");
            int color = known ? 0xFF88CC88 : level >= data.level() ? 0xFFBBBBBB : 0xFF666666;
            g.text(font, Component.literal(data.level() + "  ").append(name), x, y, color, false);
            y += 9;
        }
    }

    private long secondsLeft(BondCodex.Bond bond) {
        return Math.max(0, (bond.readyAt - minecraft.level.getGameTime()) / 20);
    }

    // The same check the server makes (BondService.nearShrine), so the button greys out honestly
    private boolean nearShrine() {
        int r = Config.BOND_SHRINE_RADIUS.get();
        BlockPos center = minecraft.player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, -r, -r), center.offset(r, r, r))) {
            if (minecraft.level.getBlockState(pos).is(ModBlocks.REKINDLING_SHRINE.get())) return true;
        }
        return false;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        for (Button b : buttons) {
            if (b.enabled() && b.contains(event.x(), event.y())) {
                ClientPacketDistributor.sendToServer(new CodexActionPayload(b.action(), b.bond()));
                minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0f));
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scroll -= (int) Math.signum(scrollY);
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (ClientEvents.BOND_CODEX.matches(event)) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
