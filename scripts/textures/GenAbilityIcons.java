// Generates the ability icons (HUD bar + Ability Book), the new status effect icons and the
// Codex item texture. Run from the project root:  java scripts/textures/GenAbilityIcons.java
//
// Each icon is a framed tile in its category colour with a glyph drawn from simple shapes
// (lines, discs, polygons) and a dark outline for readability at 16 px. Tweak the glyph functions
// below and re-run; the PNGs are committed so the build doesn't need this.
//   ability icons -> textures/gui/sprites/ability/<id>.png (16x16, stitched into the GUI atlas)
//   effect icons  -> textures/mob_effect/<id>.png (18x18)
//   codex         -> textures/item/codex.png (16x16)
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.function.Consumer;
import javax.imageio.ImageIO;

public class GenAbilityIcons {
    static final String ABILITY = "src/main/resources/assets/skillcraft/textures/gui/sprites/ability/";
    static final String EFFECT = "src/main/resources/assets/skillcraft/textures/mob_effect/";
    static final String ITEM = "src/main/resources/assets/skillcraft/textures/item/";

    // Category accents (match ability/AbilityCategory)
    static final int MELEE = 0xD04040, RANGED = 0x50B848, DEFENCE = 0x4A86E0, ART = 0xE0A030, UTILITY = 0xA060D0;
    // Glyph colours
    static final int STEEL = 0xE4E8EE, STEEL_D = 0x9AA2AE, WOOD = 0x9A6A3E, GOLD = 0xF2C14E, RED = 0xE0383A,
            WHITE = 0xFFFFFF, EMBER = 0xFF8A2A, LAVA = 0xFFC040, GREEN = 0x7ED957, CYAN = 0x7FD6E8, VIOLET = 0xC9B8F0, BLUE = 0x6FA8FF;

    public static void main(String[] args) throws Exception {
        new File(ABILITY).mkdirs();
        new File(EFFECT).mkdirs();
        new File(ITEM).mkdirs();

        // --- Melee ---
        ability("lunge", MELEE, c -> { speedLines(c, 1, 6, 3); swordH(c, 4, 8, 15); });
        ability("hamstring", MELEE, c -> { swordD(c); chevronDown(c, 11, 10, BLUE); });
        ability("rending_cut", MELEE, c -> { slash(c, WHITE); drop(c, 4, 11, RED); drop(c, 11, 4, RED); });
        ability("sweeping_arc", MELEE, c -> { arc(c, 8, 10, 6, 200, 340, WHITE, 2); swordH(c, 9, 10, 15); });
        ability("battle_trance", MELEE, c -> { flame(c, 8, 13, EMBER); crossedSwords(c); });
        // --- Ranged ---
        ability("quick_draw", RANGED, c -> { bow(c); bolt(c, 11, 2, GOLD); });
        ability("pinning_shot", RANGED, c -> { arrowDown(c, 8); c.line(3, 14, 13, 14, STEEL_D, 1); chain(c, 3, 11); });
        ability("tumble", RANGED, c -> { arc(c, 8, 9, 5, 180, 360, WHITE, 2); c.poly(new int[]{2, 6, 2}, new int[]{6, 9, 11}, WHITE); });
        ability("volley", RANGED, c -> { arrow(c, 2, 13, 13, 2); arrow(c, 2, 13, 14, 8); arrow(c, 2, 13, 8, 1); });
        ability("deadeye", RANGED, c -> { eye(c, 8, 8, GREEN); c.line(8, 1, 8, 4, WHITE, 1); c.line(8, 12, 8, 15, WHITE, 1); c.line(1, 8, 3, 8, WHITE, 1); c.line(13, 8, 15, 8, WHITE, 1); });
        // --- Defence ---
        ability("brace", DEFENCE, c -> { shield(c, STEEL_D, STEEL); c.line(5, 13, 11, 13, WHITE, 1); });
        ability("riposte", DEFENCE, c -> { shield(c, STEEL_D, STEEL); c.line(2, 14, 14, 2, GOLD, 1); });
        ability("unshackle", DEFENCE, c -> { brokenChain(c, STEEL); burst(c, 8, 8, 2, WHITE); });
        ability("second_wind", DEFENCE, c -> { heart(c, 8, 9, RED); plus(c, 12, 4, WHITE); });
        ability("bulwark", DEFENCE, c -> { shield(c, 0x2E4E8A, GOLD); spikes(c); });
        ability("last_stand", DEFENCE, c -> { shield(c, 0x8A2E2E, GOLD); c.rect(7, 4, 2, 5, WHITE); c.rect(7, 10, 2, 2, WHITE); });
        // --- Weapon Arts ---
        ability("concussive_blow", ART, c -> { hammer(c, STEEL_D); stars(c); });
        ability("earthbreaker", ART, c -> { hammer(c, STEEL_D); cracks(c); });
        ability("vault", ART, c -> { arc(c, 8, 12, 6, 180, 360, CYAN, 1); spear(c, 2, 14, 14, 3); });
        ability("skewer", ART, c -> { c.disc(6, 8, 2, RED); c.disc(11, 8, 2, RED); spear(c, 1, 8, 15, 8); });
        ability("cleaving_chop", ART, c -> axe(c));
        ability("shield_bash", ART, c -> { shield(c, STEEL_D, STEEL); impact(c, 13, 3); });
        ability("piercing_bolt", ART, c -> { c.disc(6, 8, 2, RED); c.disc(10, 8, 2, RED); arrow(c, 1, 8, 15, 8); });
        ability("forgequake", ART, c -> { lavaRing(c); maul(c); });
        ability("twinfang", ART, c -> twinBlades(c));
        ability("torchbrand", ART, c -> torch(c));
        // --- Utility ---
        ability("sprint_burst", UTILITY, c -> { speedLines(c, 1, 5, 4); wing(c, CYAN); });

        // --- Status effects (18x18, no frame, like vanilla) ---
        effect("bleeding", c -> { drop(c, 9, 10, RED); drop(c, 5, 6, RED); });
        effect("rooted", c -> { chain(c, 4, 6); chain(c, 9, 10); c.line(2, 16, 16, 16, 0x5C7A2E, 1); });
        effect("slowed", c -> { chevronDown(c, 9, 6, BLUE); chevronDown(c, 9, 11, BLUE); });
        effect("stunned", c -> stars(c));
        effect("dazed", c -> spiral(c, VIOLET));
        effect("battle_trance", c -> flame(c, 9, 15, EMBER));
        effect("braced", c -> shield(c, STEEL_D, STEEL));
        effect("bulwark", c -> { shield(c, 0x2E4E8A, GOLD); spikes(c); });
        effect("last_stand", c -> shield(c, 0x8A2E2E, GOLD));
        effect("deadeye", c -> eye(c, 9, 9, GREEN));
        effect("fleet", c -> wing(c, CYAN));
        effect("parrying", c -> { shield(c, WHITE, STEEL); });
        effect("riposte", c -> swordD(c));
        effect("unshackled", c -> brokenChain(c, STEEL));
        effect("second_wind", c -> heart(c, 9, 10, RED));

        codex();
        System.out.println("Wrote ability icons, effect icons and the codex texture");
    }

    // ---------- canvas ----------

    static final class Canvas {
        final int w, h;
        final int[] px;

        Canvas(int w, int h) {
            this.w = w;
            this.h = h;
            px = new int[w * h];
        }

        void set(int x, int y, int rgb) {
            if (x >= 0 && y >= 0 && x < w && y < h) px[y * w + x] = 0xFF000000 | rgb;
        }

        int get(int x, int y) {
            return x >= 0 && y >= 0 && x < w && y < h ? px[y * w + x] : 0;
        }

        void rect(int x, int y, int rw, int rh, int rgb) {
            for (int i = 0; i < rw; i++) for (int j = 0; j < rh; j++) set(x + i, y + j, rgb);
        }

        void disc(double cx, double cy, double r, int rgb) {
            for (int x = 0; x < w; x++) for (int y = 0; y < h; y++) {
                double dx = x + 0.5 - cx - 0.5, dy = y + 0.5 - cy - 0.5;
                if (dx * dx + dy * dy <= r * r + 0.3) set(x, y, rgb);
            }
        }

        void line(double x0, double y0, double x1, double y1, int rgb, int thickness) {
            int steps = (int) Math.ceil(Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0)) * 2) + 1;
            for (int s = 0; s <= steps; s++) {
                double t = s / (double) steps;
                double x = x0 + (x1 - x0) * t, y = y0 + (y1 - y0) * t;
                if (thickness <= 1) set((int) Math.round(x), (int) Math.round(y), rgb);
                else for (int i = 0; i < thickness; i++) for (int j = 0; j < thickness; j++)
                    set((int) Math.round(x) + i - thickness / 2, (int) Math.round(y) + j - thickness / 2, rgb);
            }
        }

        void poly(int[] xs, int[] ys, int rgb) {
            for (int x = 0; x < w; x++) for (int y = 0; y < h; y++) {
                boolean inside = false;
                for (int i = 0, j = xs.length - 1; i < xs.length; j = i++) {
                    if ((ys[i] > y + 0.5) != (ys[j] > y + 0.5)
                            && x + 0.5 < (xs[j] - xs[i]) * (y + 0.5 - ys[i]) / (double) (ys[j] - ys[i]) + xs[i]) inside = !inside;
                }
                if (inside) set(x, y, rgb);
            }
        }

        // 1 px dark outline around everything drawn so far
        void outline(int rgb) {
            int[] copy = px.clone();
            for (int x = 0; x < w; x++) for (int y = 0; y < h; y++) {
                if (copy[y * w + x] != 0) continue;
                boolean edge = false;
                for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                    int nx = x + d[0], ny = y + d[1];
                    if (nx >= 0 && ny >= 0 && nx < w && ny < h && copy[ny * w + nx] != 0) edge = true;
                }
                if (edge) px[y * w + x] = 0xFF000000 | rgb;
            }
        }

        void write(String path) throws Exception {
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            img.setRGB(0, 0, w, h, px, 0, w);
            ImageIO.write(img, "png", new File(path));
        }
    }

    static int shade(int rgb, double f) {
        int r = (int) Math.min(255, ((rgb >> 16) & 0xFF) * f), g = (int) Math.min(255, ((rgb >> 8) & 0xFF) * f), b = (int) Math.min(255, (rgb & 0xFF) * f);
        return (r << 16) | (g << 8) | b;
    }

    // Framed tile: dark base, category-tinted inner gradient, glyph with outline on top
    static void ability(String id, int accent, Consumer<Canvas> glyph) throws Exception {
        Canvas tile = new Canvas(16, 16);
        for (int x = 0; x < 16; x++) for (int y = 0; y < 16; y++) {
            boolean corner = (x == 0 || x == 15) && (y == 0 || y == 15);
            if (corner) continue;
            boolean border = x == 0 || y == 0 || x == 15 || y == 15;
            double t = (x + y) / 30.0;
            tile.set(x, y, border ? shade(accent, 0.45) : shade(accent, 0.30 + 0.28 * (1 - t)));
        }
        Canvas g = new Canvas(16, 16);
        glyph.accept(g);
        g.outline(0x101014);
        for (int i = 0; i < 256; i++) if (g.px[i] != 0) tile.px[i] = g.px[i];
        // top-left highlight
        tile.set(1, 1, shade(accent, 1.0));
        tile.write(ABILITY + id + ".png");
    }

    static void effect(String id, Consumer<Canvas> glyph) throws Exception {
        Canvas c = new Canvas(18, 18);
        Canvas g = new Canvas(16, 16);
        glyph.accept(g);
        g.outline(0x101014);
        for (int x = 0; x < 16; x++) for (int y = 0; y < 16; y++) if (g.get(x, y) != 0) c.px[(y + 1) * 18 + x + 1] = g.get(x, y);
        c.write(EFFECT + id + ".png");
    }

    static void codex() throws Exception {
        Canvas c = new Canvas(16, 16);
        c.rect(2, 2, 12, 12, 0x5A2A1A);        // cover
        c.rect(3, 3, 10, 10, 0x7A3A22);
        c.rect(2, 13, 12, 2, 0xE8DDC0);         // page edge
        c.rect(2, 2, 2, 12, 0x3E1C10);           // spine
        c.rect(6, 5, 5, 5, GOLD);                // ember emblem
        c.rect(7, 6, 3, 3, EMBER);
        c.set(8, 7, LAVA);
        c.outline(0x1A0E08);
        c.write(ITEM + "codex.png");
    }

    // ---------- glyphs ----------

    static void swordD(Canvas c) {
        c.line(4, 11, 13, 2, STEEL, 2);
        c.line(4, 11, 13, 2, WHITE, 1);
        c.line(2, 9, 6, 13, GOLD, 1);
        c.line(1, 14, 3, 12, WOOD, 2);
    }

    static void swordH(Canvas c, int x0, int y, int x1) {
        c.line(x0, y, x1 - 2, y, STEEL, 2);
        c.set(x1 - 1, y, WHITE);
        c.line(x0 - 1, y - 2, x0 - 1, y + 2, GOLD, 1);
        c.line(x0 - 4, y, x0 - 2, y, WOOD, 1);
    }

    static void crossedSwords(Canvas c) {
        c.line(3, 12, 12, 3, STEEL, 1);
        c.line(3, 3, 12, 12, STEEL, 1);
        c.line(2, 10, 5, 13, GOLD, 1);
        c.line(10, 13, 13, 10, GOLD, 1);
    }

    // Two short parallel blades: one per hand
    static void twinBlades(Canvas c) {
        c.line(3, 9, 8, 4, STEEL, 2);
        c.set(9, 3, WHITE);
        c.line(1, 8, 3, 10, GOLD, 1);
        c.line(7, 13, 12, 8, STEEL, 2);
        c.set(13, 7, WHITE);
        c.line(5, 12, 7, 14, GOLD, 1);
    }

    // A slanted torch with a flame at its tip
    static void torch(Canvas c) {
        c.line(3, 14, 8, 8, WOOD, 2);
        c.poly(new int[]{7, 8, 9, 11, 12, 13, 12}, new int[]{9, 5, 7, 2, 6, 5, 10}, EMBER);
        c.poly(new int[]{9, 10, 11}, new int[]{9, 6, 9}, LAVA);
    }

    static void slash(Canvas c, int rgb) {
        arc(c, 2, 14, 12, 270, 360, rgb, 2);
    }

    static void arc(Canvas c, double cx, double cy, double r, double from, double to, int rgb, int thickness) {
        for (double a = from; a <= to; a += 4) {
            double rad = Math.toRadians(a);
            for (int t = 0; t < thickness; t++) c.set((int) Math.round(cx + Math.cos(rad) * (r - t)), (int) Math.round(cy + Math.sin(rad) * (r - t)), rgb);
        }
    }

    static void speedLines(Canvas c, int x, int y, int n) {
        for (int i = 0; i < n; i++) c.line(x, y + i * 2, x + 3 - (i % 2), y + i * 2, WHITE, 1);
    }

    static void chevronDown(Canvas c, int cx, int cy, int rgb) {
        c.line(cx - 3, cy - 2, cx, cy + 1, rgb, 1);
        c.line(cx, cy + 1, cx + 3, cy - 2, rgb, 1);
        c.line(cx - 3, cy - 1, cx, cy + 2, rgb, 1);
        c.line(cx, cy + 2, cx + 3, cy - 1, rgb, 1);
    }

    static void drop(Canvas c, int cx, int cy, int rgb) {
        c.disc(cx - 0.5, cy, 2, rgb);
        c.poly(new int[]{cx - 2, cx, cx + 2}, new int[]{cy, cy - 4, cy}, rgb);
    }

    static void flame(Canvas c, int cx, int base, int rgb) {
        c.poly(new int[]{cx - 5, cx - 3, cx - 1, cx, cx + 2, cx + 4, cx + 5}, new int[]{base, base - 7, base - 4, base - 11, base - 5, base - 8, base}, rgb);
        c.poly(new int[]{cx - 2, cx, cx + 2}, new int[]{base, base - 6, base}, LAVA);
    }

    static void bow(Canvas c) {
        arc(c, 12, 8, 7, 120, 240, WOOD, 2);
        c.line(8, 2, 8, 14, STEEL, 1);
        c.line(3, 8, 10, 8, STEEL_D, 1);
        c.poly(new int[]{1, 4, 4}, new int[]{8, 6, 10}, STEEL);
    }

    static void bolt(Canvas c, int x, int y, int rgb) {
        c.line(x + 3, y, x, y + 4, rgb, 1);
        c.line(x, y + 4, x + 3, y + 4, rgb, 1);
        c.line(x + 3, y + 4, x, y + 8, rgb, 1);
    }

    static void arrow(Canvas c, int x0, int y0, int x1, int y1) {
        c.line(x0, y0, x1, y1, WOOD, 1);
        double dx = x1 - x0, dy = y1 - y0, len = Math.hypot(dx, dy);
        double ux = dx / len, uy = dy / len;
        c.poly(new int[]{x1 + (int) Math.round(ux), (int) Math.round(x1 - ux * 3 - uy * 2), (int) Math.round(x1 - ux * 3 + uy * 2)},
                new int[]{y1 + (int) Math.round(uy), (int) Math.round(y1 - uy * 3 + ux * 2), (int) Math.round(y1 - uy * 3 - ux * 2)}, STEEL);
        c.line(x0 - uy * 2, y0 + ux * 2, x0 + ux * 2, y0 + uy * 2, WHITE, 1);
        c.line(x0 + uy * 2, y0 - ux * 2, x0 + ux * 2, y0 + uy * 2, WHITE, 1);
    }

    static void arrowDown(Canvas c, int x) {
        c.line(x, 1, x, 11, WOOD, 1);
        c.poly(new int[]{x - 2, x + 3, x + 1}, new int[]{9, 9, 13}, STEEL);
        c.line(x - 2, 1, x, 3, WHITE, 1);
        c.line(x + 2, 1, x, 3, WHITE, 1);
    }

    static void chain(Canvas c, int x, int y) {
        for (int i = 0; i < 2; i++) {
            int ox = x + i * 4;
            c.rect(ox, y, 4, 1, STEEL_D);
            c.rect(ox, y + 3, 4, 1, STEEL_D);
            c.rect(ox, y, 1, 4, STEEL_D);
            c.rect(ox + 3, y, 1, 4, STEEL_D);
        }
    }

    static void brokenChain(Canvas c, int rgb) {
        c.rect(1, 6, 5, 1, rgb); c.rect(1, 10, 5, 1, rgb); c.rect(1, 6, 1, 5, rgb); c.rect(5, 6, 1, 2, rgb); c.rect(5, 9, 1, 2, rgb);
        c.rect(10, 6, 5, 1, rgb); c.rect(10, 10, 5, 1, rgb); c.rect(14, 6, 1, 5, rgb); c.rect(10, 6, 1, 2, rgb); c.rect(10, 9, 1, 2, rgb);
    }

    static void burst(Canvas c, int cx, int cy, int r, int rgb) {
        c.line(cx - r, cy - r, cx + r, cy + r, rgb, 1);
        c.line(cx - r, cy + r, cx + r, cy - r, rgb, 1);
        c.line(cx, cy - r - 1, cx, cy + r + 1, rgb, 1);
    }

    static void heart(Canvas c, int cx, int cy, int rgb) {
        c.disc(cx - 3, cy - 2, 2.6, rgb);
        c.disc(cx + 2, cy - 2, 2.6, rgb);
        c.poly(new int[]{cx - 6, cx + 6, cx}, new int[]{cy - 1, cy - 1, cy + 6}, rgb);
        c.set(cx - 3, cy - 3, 0xFFB0B0);
    }

    static void plus(Canvas c, int cx, int cy, int rgb) {
        c.rect(cx - 2, cy, 5, 1, rgb);
        c.rect(cx, cy - 2, 1, 5, rgb);
    }

    static void shield(Canvas c, int body, int rim) {
        c.poly(new int[]{3, 13, 13, 8, 3}, new int[]{2, 2, 9, 15, 9}, rim);
        c.poly(new int[]{4, 12, 12, 8, 4}, new int[]{3, 3, 9, 14, 9}, body);
    }

    static void spikes(Canvas c) {
        c.set(1, 4, GOLD); c.set(14, 4, GOLD); c.set(8, 0, GOLD); c.set(2, 9, GOLD); c.set(13, 9, GOLD);
    }

    static void eye(Canvas c, int cx, int cy, int iris) {
        c.poly(new int[]{cx - 6, cx, cx + 6, cx}, new int[]{cy, cy - 4, cy, cy + 4}, WHITE);
        c.disc(cx - 0.5, cy - 0.5, 2.2, iris);
        c.set(cx, cy, 0x101014);
    }

    static void hammer(Canvas c, int head) {
        c.line(4, 13, 10, 7, WOOD, 2);
        c.poly(new int[]{7, 11, 15, 11}, new int[]{4, 0, 4, 8}, head);
        c.poly(new int[]{8, 11, 14, 11}, new int[]{4, 1, 4, 7}, STEEL);
    }

    static void maul(Canvas c) {
        c.line(3, 14, 9, 8, WOOD, 2);
        c.poly(new int[]{6, 11, 15, 10}, new int[]{5, 0, 5, 10}, 0x3A3238);
        c.poly(new int[]{8, 11, 13, 10}, new int[]{5, 2, 5, 8}, EMBER);
    }

    static void lavaRing(Canvas c) {
        arc(c, 8, 9, 7, 0, 360, LAVA, 1);
    }

    static void stars(Canvas c) {
        for (int[] s : new int[][]{{3, 3}, {12, 2}, {13, 11}}) {
            c.set(s[0], s[1], GOLD); c.set(s[0] - 1, s[1], GOLD); c.set(s[0] + 1, s[1], GOLD); c.set(s[0], s[1] - 1, GOLD); c.set(s[0], s[1] + 1, GOLD);
        }
    }

    static void cracks(Canvas c) {
        c.line(1, 15, 5, 12, LAVA, 1);
        c.line(5, 12, 8, 15, LAVA, 1);
        c.line(9, 15, 13, 12, LAVA, 1);
    }

    static void spear(Canvas c, int x0, int y0, int x1, int y1) {
        c.line(x0, y0, x1, y1, WOOD, 1);
        double dx = x1 - x0, dy = y1 - y0, len = Math.hypot(dx, dy);
        double ux = dx / len, uy = dy / len;
        c.poly(new int[]{(int) Math.round(x1 + ux * 1.5), (int) Math.round(x1 - ux * 3 - uy * 2), (int) Math.round(x1 - ux * 3 + uy * 2)},
                new int[]{(int) Math.round(y1 + uy * 1.5), (int) Math.round(y1 - uy * 3 + ux * 2), (int) Math.round(y1 - uy * 3 - ux * 2)}, STEEL);
    }

    static void axe(Canvas c) {
        c.line(3, 14, 11, 4, WOOD, 2);
        c.poly(new int[]{8, 14, 15, 11}, new int[]{3, 1, 8, 9}, STEEL);
        c.line(14, 2, 15, 8, WHITE, 1);
    }

    static void impact(Canvas c, int x, int y) {
        c.line(x - 2, y, x + 2, y, WHITE, 1);
        c.line(x, y - 2, x, y + 2, WHITE, 1);
        c.set(x - 2, y - 2, WHITE); c.set(x + 2, y + 2, WHITE); c.set(x + 2, y - 2, WHITE); c.set(x - 2, y + 2, WHITE);
    }

    // A swept wing: three feathers fanning up and back from the joint
    static void wing(Canvas c, int rgb) {
        c.poly(new int[]{4, 14, 15, 9}, new int[]{13, 2, 5, 13}, rgb);
        c.poly(new int[]{6, 15, 15, 10}, new int[]{13, 6, 9, 13}, shade(rgb, 0.8));
        c.poly(new int[]{8, 14, 14, 11}, new int[]{13, 10, 12, 13}, shade(rgb, 0.65));
        c.line(4, 13, 14, 3, WHITE, 1);
    }

    static void spiral(Canvas c, int rgb) {
        for (double a = 0; a < Math.PI * 5; a += 0.15) {
            double r = a * 0.42;
            c.set((int) Math.round(8 + Math.cos(a) * r), (int) Math.round(8 + Math.sin(a) * r), rgb);
        }
    }
}
