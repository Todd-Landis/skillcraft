// Generates the Slagheart boss textures procedurally:
//   src/main/resources/assets/skillcraft/textures/entity/slagheart/slagheart.png       (base, 256x128)
//   src/main/resources/assets/skillcraft/textures/entity/slagheart/slagheart_glow.png  (emissive layer)
// Run from the project root:  java scripts/textures/GenSlagheartTexture.java
//
// The cube table below MUST match client/boss/SlagheartModel (same texture offsets and sizes).
// Each cube face is painted on its own (box UV layout), so plates get bevels, rivets and seams
// that line up with the real cube edges. Edit the palette or painters and re-run, or paint over
// the PNGs by hand.
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Random;
import javax.imageio.ImageIO;

public class GenSlagheartTexture {
    static final int W = 256, H = 128;
    static final BufferedImage BASE = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
    static final BufferedImage GLOW = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
    static final Random RNG = new Random(0x51A6);

    enum Mat { IRON, STEEL, LEATHER, BRASS, HEART, SOOT }
    enum Face { TOP, BOTTOM, SIDE_A, FRONT, SIDE_B, BACK }

    // name, u, v, width, height, depth, material  (keep in sync with SlagheartModel)
    record Box(String name, int u, int v, int w, int h, int d, Mat mat) {}
    static final Box[] BOXES = {
            new Box("torso", 0, 0, 24, 24, 16, Mat.IRON),
            new Box("hammer", 80, 0, 14, 10, 16, Mat.STEEL),
            new Box("hips", 140, 0, 16, 6, 10, Mat.IRON),
            new Box("bellows", 192, 0, 16, 14, 4, Mat.LEATHER),
            new Box("head", 140, 16, 10, 9, 10, Mat.IRON),
            new Box("door", 180, 18, 7, 13, 2, Mat.IRON),
            new Box("brow", 198, 18, 12, 3, 4, Mat.IRON),
            new Box("chimney_cap", 198, 26, 5, 2, 5, Mat.SOOT),
            new Box("heart", 218, 26, 10, 10, 1, Mat.HEART),
            new Box("leg", 0, 40, 7, 14, 8, Mat.IRON),
            new Box("foot", 30, 40, 9, 4, 11, Mat.SOOT),
            new Box("pauldron", 80, 26, 9, 9, 12, Mat.IRON),
            new Box("upper_arm", 122, 40, 8, 14, 8, Mat.IRON),
            new Box("forearm", 154, 40, 9, 12, 9, Mat.IRON),
            new Box("wrist", 190, 40, 8, 4, 8, Mat.BRASS),
            new Box("chimney", 222, 40, 4, 12, 4, Mat.SOOT),
            new Box("prong", 238, 40, 4, 14, 3, Mat.STEEL),
            new Box("knee", 0, 62, 9, 5, 3, Mat.BRASS),
            new Box("crest", 24, 62, 2, 4, 8, Mat.BRASS),
            new Box("belly", 44, 62, 18, 4, 2, Mat.IRON),
    };

    public static void main(String[] args) throws Exception {
        checkOverlaps();
        for (Box box : BOXES) paintBox(box);
        File dir = new File("src/main/resources/assets/skillcraft/textures/entity/slagheart");
        dir.mkdirs();
        ImageIO.write(BASE, "png", new File(dir, "slagheart.png"));
        ImageIO.write(GLOW, "png", new File(dir, "slagheart_glow.png"));
        System.out.println("Wrote slagheart.png and slagheart_glow.png to " + dir);
    }

    // --- palette ---
    static int[] palette(Mat mat) {
        return switch (mat) {           // base, light, dark
            case IRON -> new int[]{0x3B3F45, 0x5E646C, 0x22252A};
            case STEEL -> new int[]{0x5A5F66, 0x8C939B, 0x34373C};
            case LEATHER -> new int[]{0x4A3226, 0x6B4A37, 0x2C1C15};
            case BRASS -> new int[]{0x8A6A2E, 0xC29A48, 0x553F18};
            case HEART -> new int[]{0xFF8A1E, 0xFFE07A, 0xC2410C};
            case SOOT -> new int[]{0x2A2624, 0x423B37, 0x171413};
        };
    }

    static void paintBox(Box b) {
        int u = b.u, v = b.v, w = b.w, h = b.h, d = b.d;
        paintFace(b, Face.TOP, u + d, v, w, d);
        paintFace(b, Face.BOTTOM, u + d + w, v, w, d);
        paintFace(b, Face.SIDE_A, u, v + d, d, h);
        paintFace(b, Face.FRONT, u + d, v + d, w, h);
        paintFace(b, Face.SIDE_B, u + d + w, v + d, d, h);
        paintFace(b, Face.BACK, u + 2 * d + w, v + d, w, h);
    }

    static void paintFace(Box b, Face face, int x0, int y0, int fw, int fh) {
        if (fw <= 0 || fh <= 0) return;
        int[] p = palette(b.mat);
        // Base fill with gentle noise
        for (int y = 0; y < fh; y++) {
            for (int x = 0; x < fw; x++) {
                double n = (RNG.nextDouble() - 0.5) * 0.16;
                int c = shade(p[0], n);
                if (b.mat == Mat.LEATHER && y % 3 == 2) c = p[2];                // bellows pleats
                if (b.mat == Mat.SOOT && RNG.nextDouble() < 0.12) c = p[2];
                if (b.mat == Mat.HEART) c = heartPixel(x, y, fw, fh);
                set(BASE, x0 + x, y0 + y, c);
            }
        }
        if (b.mat == Mat.HEART) {
            for (int y = 0; y < fh; y++) for (int x = 0; x < fw; x++) set(GLOW, x0 + x, y0 + y, heartPixel(x, y, fw, fh));
            return;
        }
        // Plate seams on large iron faces
        if (b.mat == Mat.IRON && fh >= 10) {
            for (int y = 6; y < fh - 2; y += 7) {
                for (int x = 1; x < fw - 1; x++) {
                    set(BASE, x0 + x, y0 + y, p[2]);
                    set(BASE, x0 + x, y0 + y + 1, shade(p[1], -0.1));
                }
            }
        }
        // Bevel: light top/left edge, dark bottom/right edge
        for (int x = 0; x < fw; x++) {
            set(BASE, x0 + x, y0, shade(p[1], 0.05));
            set(BASE, x0 + x, y0 + fh - 1, p[2]);
        }
        for (int y = 0; y < fh; y++) {
            set(BASE, x0, y0 + y, shade(p[1], 0));
            set(BASE, x0 + fw - 1, y0 + y, p[2]);
        }
        // Rivets in the corners of plate faces
        if ((b.mat == Mat.IRON || b.mat == Mat.BRASS) && fw >= 6 && fh >= 6) {
            rivet(x0 + 2, y0 + 2);
            rivet(x0 + fw - 3, y0 + 2);
            rivet(x0 + 2, y0 + fh - 3);
            rivet(x0 + fw - 3, y0 + fh - 3);
        }
        // Hot cracks on iron (base + glow)
        if (b.mat == Mat.IRON && fw * fh > 40 && RNG.nextDouble() < 0.55) crack(x0, y0, fw, fh, 4 + RNG.nextInt(6));

        details(b, face, x0, y0, fw, fh);
    }

    // Per-part details: the grille behind the doors, the visor, the hammer's striking face...
    static void details(Box b, Face face, int x0, int y0, int fw, int fh) {
        switch (b.name) {
            case "torso" -> {
                if (face == Face.FRONT) {
                    // Recessed furnace grille (columns 5..18, rows 5..18) with glowing slits
                    for (int y = 5; y <= 18; y++) for (int x = 5; x <= 18; x++) {
                        boolean slit = (x % 2 == 1) && y > 5 && y < 18;
                        set(BASE, x0 + x, y0 + y, slit ? 0xFF6A12 : 0x161414);
                        if (slit) set(GLOW, x0 + x, y0 + y, 0xFFB040);
                    }
                    // Brass collar across the chest top
                    for (int x = 1; x < fw - 1; x++) { set(BASE, x0 + x, y0 + 2, 0xC29A48); set(BASE, x0 + x, y0 + 3, 0x8A6A2E); }
                } else if (face == Face.BACK) {
                    // Exhaust vents on the back
                    for (int y = 4; y < 20; y += 4) for (int x = 6; x < fw - 6; x++) {
                        set(BASE, x0 + x, y0 + y, 0x101010);
                        if (x % 3 == 0) set(GLOW, x0 + x, y0 + y, 0xC24A10);
                    }
                }
            }
            case "head" -> {
                if (face == Face.FRONT) {
                    // Visor slit, glowing
                    for (int x = 1; x < fw - 1; x++) {
                        set(BASE, x0 + x, y0 + 4, 0xFF9A2A);
                        set(GLOW, x0 + x, y0 + 4, x == 3 || x == fw - 4 ? 0xFFF0B0 : 0xFF9A2A);
                    }
                    for (int x = 2; x < fw - 2; x++) set(BASE, x0 + x, y0 + 5, 0x141210);
                    // Breathing grate
                    for (int x = 3; x < fw - 3; x += 2) for (int y = 6; y < fh - 1; y++) set(BASE, x0 + x, y0 + y, 0x1A1816);
                }
            }
            case "door" -> {
                if (face == Face.FRONT || face == Face.BACK) {
                    // Hinge straps and a heat-warped handle
                    for (int x = 1; x < fw - 1; x++) { set(BASE, x0 + x, y0 + 3, 0x8A6A2E); set(BASE, x0 + x, y0 + fh - 4, 0x8A6A2E); }
                    set(BASE, x0 + fw / 2, y0 + fh / 2, 0xC29A48);
                    set(BASE, x0 + fw / 2, y0 + fh / 2 + 1, 0x553F18);
                }
            }
            case "hammer" -> {
                if (face == Face.BOTTOM || face == Face.TOP) {
                    // Polished striking faces with a hot edge
                    for (int y = 1; y < fh - 1; y++) for (int x = 1; x < fw - 1; x++) set(BASE, x0 + x, y0 + y, shade(0x9AA1A9, (RNG.nextDouble() - 0.5) * 0.1));
                    if (face == Face.BOTTOM) for (int x = 1; x < fw - 1; x++) { set(BASE, x0 + x, y0 + fh - 2, 0xFF7A1A); set(GLOW, x0 + x, y0 + fh - 2, 0xFF7A1A); }
                } else {
                    // Anvil-like horn line and a slag-iron band
                    for (int x = 0; x < fw; x++) set(BASE, x0 + x, y0 + fh / 2, 0x2A2D31);
                    crack(x0, y0, fw, fh, 5);
                }
            }
            case "chimney" -> {
                if (face == Face.TOP) for (int y = 1; y < fh - 1; y++) for (int x = 1; x < fw - 1; x++) { set(BASE, x0 + x, y0 + y, 0x0E0B0A); set(GLOW, x0 + x, y0 + y, 0xA8360C); }
            }
            case "chimney_cap" -> {
                if (face == Face.TOP) for (int y = 1; y < fh - 1; y++) for (int x = 1; x < fw - 1; x++) set(BASE, x0 + x, y0 + y, 0x0E0B0A);
            }
            case "pauldron" -> {
                // Brass rim along the bottom edge of every side
                if (face != Face.TOP && face != Face.BOTTOM) for (int x = 0; x < fw; x++) { set(BASE, x0 + x, y0 + fh - 2, 0xC29A48); set(BASE, x0 + x, y0 + fh - 1, 0x553F18); }
            }
            case "forearm" -> {
                if (face != Face.TOP && face != Face.BOTTOM) for (int x = 0; x < fw; x++) { set(BASE, x0 + x, y0 + 2, 0x8A6A2E); set(BASE, x0 + x, y0 + fh - 3, 0x8A6A2E); }
            }
            case "prong" -> {
                if (face == Face.FRONT || face == Face.SIDE_A || face == Face.SIDE_B) {
                    // Glowing tips where the tongs hold hot metal
                    for (int y = fh - 3; y < fh; y++) for (int x = 0; x < fw; x++) { set(BASE, x0 + x, y0 + y, 0xE0601A); set(GLOW, x0 + x, y0 + y, 0xFF8A2A); }
                }
            }
            default -> {}
        }
    }

    // Molten heart: bright core fading to deep orange, with darker crust specks
    static int heartPixel(int x, int y, int fw, int fh) {
        double cx = (fw - 1) / 2.0, cy = (fh - 1) / 2.0;
        double dist = Math.hypot((x - cx) / Math.max(1, cx), (y - cy) / Math.max(1, cy));
        int c = lerp(0xFFF2B0, 0xE0400C, Math.min(1, dist));
        if (((x * 7 + y * 13) % 11) == 0) c = lerp(c, 0x7A1E06, 0.6);
        return c;
    }

    static void crack(int x0, int y0, int fw, int fh, int len) {
        int x = 1 + RNG.nextInt(Math.max(1, fw - 2)), y = 1 + RNG.nextInt(Math.max(1, fh - 2));
        for (int i = 0; i < len; i++) {
            set(BASE, x0 + x, y0 + y, 0xE8661A);
            set(GLOW, x0 + x, y0 + y, i % 3 == 0 ? 0xFFC060 : 0xF07A20);
            x = Math.clamp(x + RNG.nextInt(3) - 1, 1, fw - 2);
            y = Math.clamp(y + (RNG.nextBoolean() ? 1 : 0), 1, fh - 2);
        }
    }

    static void rivet(int x, int y) {
        set(BASE, x, y, 0x9097A0);
        set(BASE, x + 1, y + 1, 0x1A1C1F);
    }

    static int shade(int rgb, double amount) {
        int r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, b = rgb & 255;
        r = (int) Math.clamp(r + amount * 255, 0, 255);
        g = (int) Math.clamp(g + amount * 255, 0, 255);
        b = (int) Math.clamp(b + amount * 255, 0, 255);
        return (r << 16) | (g << 8) | b;
    }

    static int lerp(int a, int b, double t) {
        int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return (r << 16) | (g << 8) | bl;
    }

    static void set(BufferedImage img, int x, int y, int rgb) {
        if (x >= 0 && y >= 0 && x < W && y < H) img.setRGB(x, y, 0xFF000000 | rgb);
    }

    // Fails loudly if two cubes' texture regions overlap (a typo in the table)
    static void checkOverlaps() {
        for (int i = 0; i < BOXES.length; i++) {
            for (int j = i + 1; j < BOXES.length; j++) {
                Box a = BOXES[i], b = BOXES[j];
                int aw = 2 * (a.d + a.w), ah = a.d + a.h, bw = 2 * (b.d + b.w), bh = b.d + b.h;
                boolean overlap = a.u < b.u + bw && b.u < a.u + aw && a.v < b.v + bh && b.v < a.v + ah;
                if (overlap) throw new IllegalStateException("UV overlap: " + a.name + " / " + b.name);
            }
            Box a = BOXES[i];
            if (a.u + 2 * (a.d + a.w) > W || a.v + a.d + a.h > H) throw new IllegalStateException("Out of texture: " + a.name);
        }
    }
}
