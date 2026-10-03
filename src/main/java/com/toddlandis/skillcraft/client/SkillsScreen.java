package com.toddlandis.skillcraft.client;

import java.util.List;

import com.toddlandis.skillcraft.registry.ModAttachments;
import com.toddlandis.skillcraft.skill.PlayerSkills;
import com.toddlandis.skillcraft.skill.Skill;
import com.toddlandis.skillcraft.skill.SkillManager;
import com.toddlandis.skillcraft.skill.XpTable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

// The skills panel (default key: K). A grid of every skill with its level and progress to the next level.
public class SkillsScreen extends Screen {
    private static final int COLUMNS = 3;
    private static final int CELL_W = 104;
    private static final int CELL_H = 26;
    private static final int GAP = 4;
    private static final int HEADER = 22;
    private static final int FOOTER = 18;

    public SkillsScreen() {
        super(Component.translatable("screen.skillcraft.skills"));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        if (minecraft.player == null) return;
        PlayerSkills skills = SkillManager.get(minecraft.player);

        Skill[] all = Skill.values();
        int rows = (all.length + COLUMNS - 1) / COLUMNS;
        int panelW = COLUMNS * CELL_W + (COLUMNS - 1) * GAP + 16;
        int panelH = HEADER + rows * CELL_H + (rows - 1) * GAP + FOOTER + 8;
        int left = (width - panelW) / 2;
        int top = (height - panelH) / 2;

        graphics.fill(left, top, left + panelW, top + panelH, 0xE0101018);
        graphics.centeredText(font, title, width / 2, top + 8, 0xFFFFFFFF);

        Skill hovered = null;
        for (int i = 0; i < all.length; i++) {
            Skill skill = all[i];
            int x = left + 8 + (i % COLUMNS) * (CELL_W + GAP);
            int y = top + HEADER + (i / COLUMNS) * (CELL_H + GAP);
            boolean isHovered = mouseX >= x && mouseX < x + CELL_W && mouseY >= y && mouseY < y + CELL_H;
            if (isHovered) hovered = skill;
            drawCell(graphics, skill, skills, x, y, isHovered);
        }

        Component total = Component.translatable("screen.skillcraft.total_level", skills.totalLevel())
                .append("   ")
                .append(Component.translatable("screen.skillcraft.attack_style", minecraft.player.getData(ModAttachments.ATTACK_STYLE).displayName(),
                        ClientEvents.ATTACK_STYLE.getTranslatedKeyMessage()));
        graphics.centeredText(font, total, width / 2, top + panelH - FOOTER + 2, 0xFFFFD700);

        if (hovered != null) {
            graphics.setTooltipForNextFrame(font, tooltipFor(hovered, skills), java.util.Optional.empty(), net.minecraft.world.item.ItemStack.EMPTY, mouseX, mouseY);
        }
    }

    private void drawCell(GuiGraphicsExtractor graphics, Skill skill, PlayerSkills skills, int x, int y, boolean hovered) {
        int level = skills.getLevel(skill);
        graphics.fill(x, y, x + CELL_W, y + CELL_H, hovered ? 0xFF3A3A4A : 0xFF24242E);

        graphics.text(font, skill.displayName(), x + 4, y + 4, 0xFFFFFFFF, true);
        String levelText = level + "/" + XpTable.MAX_LEVEL;
        graphics.text(font, levelText, x + CELL_W - 4 - font.width(levelText), y + 4, 0xFFFFFF55, true);

        // Progress bar to the next level
        int barX = x + 4, barY = y + CELL_H - 7, barW = CELL_W - 8;
        graphics.fill(barX, barY, barX + barW, barY + 3, 0xFF000000);
        graphics.fill(barX, barY, barX + (int) (barW * progress(skills, skill)), barY + 3, categoryColor(skill.category()));
    }

    private static float progress(PlayerSkills skills, Skill skill) {
        int level = skills.getLevel(skill);
        if (level >= XpTable.MAX_LEVEL) return 1f;
        long start = XpTable.xpForLevel(level);
        long end = XpTable.xpForLevel(level + 1);
        return (float) ((skills.getXp(skill) - start) / (end - start));
    }

    private static List<Component> tooltipFor(Skill skill, PlayerSkills skills) {
        double xp = skills.getXp(skill);
        int level = skills.getLevel(skill);
        if (level >= XpTable.MAX_LEVEL) {
            return List.of(skill.displayName().copy().withStyle(ChatFormatting.GOLD),
                    Component.translatable("screen.skillcraft.xp", XpTable.format(xp)));
        }
        long next = XpTable.xpForLevel(level + 1);
        return List.of(
                skill.displayName().copy().withStyle(ChatFormatting.GOLD),
                Component.translatable("screen.skillcraft.xp", XpTable.format(xp)),
                Component.translatable("screen.skillcraft.next_level", XpTable.format(next)).withStyle(ChatFormatting.GRAY),
                Component.translatable("screen.skillcraft.remaining", XpTable.format(next - xp)).withStyle(ChatFormatting.GRAY));
    }

    private static int categoryColor(Skill.Category category) {
        return switch (category) {
            case COMBAT -> 0xFFD04040;
            case GATHERING -> 0xFF40B040;
            case ARTISAN -> 0xFFE09030;
            case SUPPORT -> 0xFF4080E0;
        };
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        // Pressing the skills key again closes the screen
        if (ClientEvents.OPEN_SKILLS.matches(event)) {
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
