// Generates textures for Fishing and Cooking: the shoal items, cooked/burnt food, and the
// ripple and foam layers drawn on the water by FishingSpotRenderer.
// Run from the project root:  java scripts/textures/GenFishingCookingTextures.java
//
// Everything is drawn procedurally (shapes plus shading) so it is easy to tweak the numbers.
// Re-run after editing; the PNGs are committed so the build doesn't need this.
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Random;
import javax.imageio.ImageIO;

public class GenFishingCookingTextures {
    static final String ITEMS = "src/main/resources/assets/skillcraft/textures/item/";
    static final String SPOT = "src/main/resources/assets/skillcraft/textures/entity/fishing_spot/";

    public static void main(String[] args) throws Exception {
        new File(ITEMS).mkdirs();
        new File(SPOT).mkdirs();

        // Shoals: a bubble of water with the fish inside (fish palette: outline, dark, mid, light)
        shoal("cod_shoal", new int[]{0x3A3024, 0x6E5E46, 0x9C8A68, 0xCBBE9C});
        shoal("salmon_shoal", new int[]{0x4A1E1C, 0x9A3A32, 0xC85A48, 0xE89A80});
        shoal("tropical_fish_shoal", new int[]{0x4A2408, 0xD8641A, 0xF29A3A, 0xFFF2E0});
        shoal("pufferfish_shoal", new int[]{0x4A3A0A, 0xB89A1E, 0xE6CC3A, 0xFFF4A8});

        // Cooked reef fish: browned, with grill marks
        BufferedImage seared = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        fish(seared, 7.5, 8.5, 6.2, 3.6, new int[]{0x2E1A0E, 0x7A3E16, 0xB0642A, 0xD8944E}, true);
        write(seared, ITEMS + "seared_reef_fish.png");

        prepared();
        burnt();
        ripples();
        foam();
        System.out.println("Wrote fishing and cooking textures.");
    }

    // --- Fish: an oval body facing left with a forked tail, outlined and shaded ---
    static void fish(BufferedImage img, double cx, double cy, double rx, double ry, int[] p, boolean grill) {
        int w = img.getWidth();
        boolean[][] in = new boolean[w][w];
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < w; y++) {
                double dx = (x + 0.5 - cx) / rx, dy = (y + 0.5 - cy) / ry;
                boolean body = dx * dx + dy * dy <= 1.0;
                // Tail: a fork behind the body
                double tx = x + 0.5 - (cx + rx - 0.5);
                boolean tail = tx >= 0 && tx <= ry * 0.9 && Math.abs(y + 0.5 - cy) <= 0.6 + tx * 0.9 && Math.abs(y + 0.5 - cy) >= tx * 0.35;
                in[x][y] = body || tail;
            }
        }
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < w; y++) {
                if (!in[x][y]) continue;
                boolean edge = x == 0 || y == 0 || x == w - 1 || y == w - 1 || !in[x - 1][y] || !in[x + 1][y] || !in[x][y - 1] || !in[x][y + 1];
                int c;
                if (edge) c = p[0];
                else if (y + 0.5 < cy - ry * 0.35) c = p[1];      // back
                else if (y + 0.5 > cy + ry * 0.3) c = p[3];       // belly
                else c = p[2];
                if (grill && !edge && (x + y) % 4 == 0 && x < cx + rx - 1) c = p[0];
                px(img, x, y, c);
            }
        }
        // Eye
        int ex = (int) Math.round(cx - rx * 0.6), ey = (int) Math.round(cy - ry * 0.25);
        px(img, ex, ey, 0x101010);
        px(img, ex, ey - 1, 0xF0F0F0);
    }

    static void shoal(String name, int[] fish) throws Exception {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        double c = 7.5, r = 7.2;
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                double d = Math.hypot(x + 0.5 - 8, y + 0.5 - 8);
                if (d > r) continue;
                double t = (y + 0.5) / 16.0;
                int col = lerp(0x5EB6E8, 0x1E5A9A, t);
                if (d > r - 1.1) col = 0x163C66;          // rim
                img.setRGB(x, y, 0xD0000000 | col);
            }
        }
        // A highlight and a couple of bubbles
        px(img, 4, 4, 0xE8F6FF); px(img, 5, 4, 0xBFE4FA); px(img, 4, 5, 0xBFE4FA);
        px(img, 11, 3, 0xD8F0FF); px(img, 12, 5, 0xD8F0FF);
        // Two small dark fish behind and the main fish in front
        px(img, 3, 11, 0x123A5E); px(img, 4, 11, 0x123A5E); px(img, 5, 12, 0x123A5E);
        px(img, 11, 12, 0x123A5E); px(img, 12, 12, 0x123A5E); px(img, 10, 11, 0x123A5E);
        fish(img, 6.6, 8.6, 4.9, 3.0, fish, false);
        write(img, ITEMS + name + ".png");
    }

    // Three pale fillets fanned out
    static void prepared() throws Exception {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        int[][] centers = {{4, 9}, {8, 7}, {12, 9}};
        for (int[] ctr : centers) {
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    double dx = (x + 0.5 - ctr[0]) / 2.3, dy = (y + 0.5 - ctr[1]) / 4.6;
                    double d = dx * dx + dy * dy;
                    if (d > 1) continue;
                    int col = d > 0.62 ? 0xB8552E : (dx < -0.2 ? 0xF4C8A6 : 0xEBA67E);
                    px(img, x, y, col);
                }
            }
        }
        // A sprig of green to show it's been prepared
        px(img, 7, 13, 0x3E8A2E); px(img, 8, 12, 0x5CB040); px(img, 9, 13, 0x3E8A2E); px(img, 8, 14, 0x2E6A22);
        write(img, ITEMS + "prepared_pufferfish.png");
    }

    // A charred lump with a few glowing embers
    static void burnt() throws Exception {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Random rnd = new Random(7);
        boolean[][] in = new boolean[16][16];
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                double a = Math.atan2(y + 0.5 - 8.5, x + 0.5 - 8);
                double r = 5.6 + 0.9 * Math.sin(a * 3 + 1) + 0.5 * Math.sin(a * 5);
                double dx = (x + 0.5 - 8) / 1.15, dy = (y + 0.5 - 8.5);
                in[x][y] = Math.hypot(dx, dy) <= r;
            }
        }
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                if (!in[x][y]) continue;
                boolean edge = !in[Math.max(0, x - 1)][y] || !in[Math.min(15, x + 1)][y] || !in[x][Math.max(0, y - 1)] || !in[x][Math.min(15, y + 1)];
                int col = edge ? 0x0A0806 : (rnd.nextInt(4) == 0 ? 0x3A322C : (y < 8 ? 0x2A2420 : 0x1C1814));
                px(img, x, y, col);
            }
        }
        px(img, 6, 8, 0xC2410C); px(img, 10, 10, 0xE0601A); px(img, 8, 12, 0x8A2A0A); px(img, 9, 6, 0x6E5E54);
        write(img, ITEMS + "burnt_food.png");
    }

    // Broken concentric rings, brightest in the middle rings and fading at the edge
    static void ripples() throws Exception {
        int n = 64;
        BufferedImage img = new BufferedImage(n, n, BufferedImage.TYPE_INT_ARGB);
        double[] radii = {9, 16, 23, 29};
        double[] strength = {0.9, 1.0, 0.75, 0.45};
        for (int x = 0; x < n; x++) {
            for (int y = 0; y < n; y++) {
                double dx = x + 0.5 - n / 2.0, dy = y + 0.5 - n / 2.0;
                double d = Math.hypot(dx, dy), a = Math.atan2(dy, dx);
                double alpha = 0;
                for (int i = 0; i < radii.length; i++) {
                    double ring = Math.exp(-Math.pow(d - radii[i], 2) / (2 * 1.1 * 1.1));
                    double arcs = 0.45 + 0.55 * Math.max(0, Math.sin(a * (3 + i) + i * 1.7));
                    alpha += ring * strength[i] * arcs;
                }
                alpha *= Math.max(0, 1 - Math.pow(d / 31.5, 6));
                int al = (int) Math.round(Math.min(1, alpha) * 235);
                if (al > 2) img.setRGB(x, y, al << 24 | 0xF2FAFF);
            }
        }
        write(img, SPOT + "ripples.png");
    }

    // Clumps of white foam clustered towards the centre
    static void foam() throws Exception {
        int n = 64;
        BufferedImage img = new BufferedImage(n, n, BufferedImage.TYPE_INT_ARGB);
        double[][] field = new double[n][n];
        Random rnd = new Random(42);
        for (int i = 0; i < 90; i++) {
            double dist = Math.abs(rnd.nextGaussian()) * 12;
            double ang = rnd.nextDouble() * Math.PI * 2;
            double bx = n / 2.0 + Math.cos(ang) * dist, by = n / 2.0 + Math.sin(ang) * dist;
            double br = 1.6 + rnd.nextDouble() * 3.2;
            for (int x = 0; x < n; x++) {
                for (int y = 0; y < n; y++) {
                    double d = Math.hypot(x + 0.5 - bx, y + 0.5 - by);
                    if (d < br) field[x][y] = Math.max(field[x][y], 1 - d / br);
                }
            }
        }
        for (int x = 0; x < n; x++) {
            for (int y = 0; y < n; y++) {
                double v = field[x][y];
                if (v <= 0) continue;
                double edge = Math.max(0, 1 - Math.pow(Math.hypot(x + 0.5 - n / 2.0, y + 0.5 - n / 2.0) / 31.5, 4));
                int al = (int) Math.round(Math.min(1, v * 1.8) * 220 * edge);
                // Bubble rims are a touch bluer than the white foam tops
                int col = v < 0.35 ? 0xD6ECFA : 0xFFFFFF;
                if (al > 2) img.setRGB(x, y, al << 24 | col);
            }
        }
        write(img, SPOT + "foam.png");
    }

    static int lerp(int a, int b, double t) {
        int r = (int) ((a >> 16 & 255) + ((b >> 16 & 255) - (a >> 16 & 255)) * t);
        int g = (int) ((a >> 8 & 255) + ((b >> 8 & 255) - (a >> 8 & 255)) * t);
        int bl = (int) ((a & 255) + ((b & 255) - (a & 255)) * t);
        return r << 16 | g << 8 | bl;
    }

    static void px(BufferedImage img, int x, int y, int rgb) {
        if (x >= 0 && y >= 0 && x < img.getWidth() && y < img.getHeight()) img.setRGB(x, y, 0xFF000000 | rgb);
    }

    static void write(BufferedImage img, String path) throws Exception {
        ImageIO.write(img, "png", new File(path));
    }
}
