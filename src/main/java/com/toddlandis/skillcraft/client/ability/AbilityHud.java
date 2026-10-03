package com.toddlandis.skillcraft.client.ability;

import com.toddlandis.skillcraft.ability.AbilityClass;
import com.toddlandis.skillcraft.ability.AbilityData;
import com.toddlandis.skillcraft.ability.AbilityDefinition;

import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;

// The 4-slot ability bar and Momentum bar on the HUD (spec 2.1). It sits right of the hotbar,
// clear of the offhand slot and attack indicator; on narrow screens it becomes a 2x2 grid, and if
// even that doesn't fit, a row above the hunger/air bars. Each slot shows the icon, its key, a
// cooldown sweep, the global cooldown, a windup bar, and is dimmed when it can't be used now.
public final class AbilityHud {
    public static final int SLOT = 20, GAP = 2, MOMENTUM_H = 3;

    public record Layout(int x, int y, int columns) {
        public int width() {
            return columns * SLOT + (columns - 1) * GAP;
        }

        public int slotX(int slot) {
            return x + (slot % columns) * (SLOT + GAP);
        }

        public int slotY(int slot) {
            return y + (slot / columns) * (SLOT + GAP);
        }
    }

    private AbilityHud() {}

    public static Layout layout(Minecraft mc, int width, int height) {
        int hotbarRight = width / 2 + 91;
        int reserve = 4;
        Player player = mc.player;
        boolean leftHanded = player != null && player.getMainArm() == HumanoidArm.LEFT;
        if (leftHanded) reserve += 29; // offhand slot moves to the right
        else if (mc.options.attackIndicator().get() == AttackIndicatorStatus.HOTBAR) reserve += 24;
        int x = hotbarRight + reserve;
        int rowWidth = AbilityData.SLOTS * SLOT + (AbilityData.SLOTS - 1) * GAP;
        if (x + rowWidth + 2 <= width) return new Layout(x, height - SLOT - 1, AbilityData.SLOTS);
        int gridWidth = 2 * SLOT + GAP;
        if (x + gridWidth + 2 <= width) return new Layout(x, height - 2 * SLOT - GAP - 1, 2);
        int y = height - mc.gui.hud.rightHeight - SLOT - MOMENTUM_H - 6;
        return new Layout(hotbarRight - rowWidth, y, AbilityData.SLOTS);
    }

    public static void render(GuiGraphicsExtractor g, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || player.isSpectator() || mc.gui.hud.isHidden()) return;
        AbilityData data = ClientAbilities.data(player);
        Layout layout = layout(mc, g.guiWidth(), g.guiHeight());
        draw(g, mc, player, data, layout);
    }

    static void draw(GuiGraphicsExtractor g, Minecraft mc, Player player, AbilityData data, Layout layout) {
        // Momentum: a thin bar over the slots, with a notch at 50 (Surge) and gold when full (Ultimate)
        int barY = layout.y() - MOMENTUM_H - 2;
        int barW = layout.width();
        float momentum = data.momentum();
        g.fill(layout.x() - 1, barY - 1, layout.x() + barW + 1, barY + MOMENTUM_H + 1, 0xC0101014);
        int filled = Math.round(barW * momentum / AbilityData.MAX_MOMENTUM);
        g.fill(layout.x(), barY, layout.x() + filled, barY + MOMENTUM_H, momentum >= AbilityData.MAX_MOMENTUM ? 0xFFFFD34A : 0xFFE0782A);
        g.fill(layout.x() + barW / 2, barY, layout.x() + barW / 2 + 1, barY + MOMENTUM_H, 0xFF000000);

        for (int slot = 0; slot < AbilityData.SLOTS; slot++) {
            Identifier id = data.activeSlot(slot);
            drawSlot(g, mc.font, player, id, AbilityKeys.label(slot), layout.slotX(slot), layout.slotY(slot));
        }
    }

    public static void drawSlot(GuiGraphicsExtractor g, Font font, Player player, Identifier id, String key, int x, int y) {
        AbilityDefinition def = ClientAbilities.get(id);
        g.fill(x, y, x + SLOT, y + SLOT, 0xB0000000);
        int border = 0xFF3A3A44;
        if (def != null) {
            boolean usable = ClientAbilities.usable(player, id, def);
            g.blitSprite(RenderPipelines.GUI_TEXTURED, def.icon(), x + 2, y + 2, 16, 16, usable ? 0xFFFFFFFF : 0xFF5A5A5A);

            float cooldown = ClientAbilities.cooldownFraction(id);
            if (cooldown > 0) {
                int h = Math.max(1, Math.round(16 * cooldown));
                g.fill(x + 2, y + 18 - h, x + 18, y + 18, 0xB0000000);
                int ticks = ClientAbilities.cooldownTicksLeft(id);
                if (ticks >= 20) {
                    String seconds = String.valueOf((ticks + 19) / 20);
                    g.text(font, seconds, x + SLOT / 2 - font.width(seconds) / 2 + 1, y + 7, 0xFFFFFFFF, true);
                }
            } else if (!def.offGcd() && ClientAbilities.gcdFraction() > 0) {
                g.fill(x + 2, y + 2, x + 18, y + 18, 0x40000000);
                g.fill(x + 2, y + 2, x + 2 + Math.round(16 * ClientAbilities.gcdFraction()), y + 3, 0xC0FFFFFF);
            }
            if (id.equals(ClientAbilities.windupId())) {
                g.fill(x + 2, y + 16, x + 18, y + 18, 0xFF202020);
                g.fill(x + 2, y + 16, x + 2 + Math.round(16 * ClientAbilities.windupProgress()), y + 18, 0xFFFFD34A);
                border = 0xFFFFD34A;
            }
            if (ClientAbilities.isToggled(id)) border = 0xFF6FE06F;
            if (def.abilityClass() == AbilityClass.ULTIMATE && ClientAbilities.pinging(id) && (ClientAbilities.now() / 3) % 2 == 0) border = 0xFFFFE680;
        }
        g.outline(x, y, SLOT, SLOT, border);
        // Key label, slightly smaller than normal text
        g.pose().pushMatrix();
        g.pose().translate(x + 1.5f, y + 1.5f);
        g.pose().scale(0.75f);
        g.text(font, key, 0, 0, def == null ? 0xFF8A8A94 : 0xFFFFFFFF, true);
        g.pose().popMatrix();
    }
}
