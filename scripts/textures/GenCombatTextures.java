// Generates item, armour-layer and effect textures for the Phase 5 combat gear and boss items.
// Run from the project root:  java scripts/textures/GenCombatTextures.java
//
// Sprites are drawn as ASCII grids (one character per pixel, '.' = transparent) with a palette
// per sprite, so they are easy to tweak by hand. Warhammers are drawn from one shape and a
// palette per metal. Re-run after editing; the PNGs are committed so the build doesn't need this.
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import javax.imageio.ImageIO;

public class GenCombatTextures {
    static final String ITEMS = "src/main/resources/assets/skillcraft/textures/item/";
    static final String EQUIP = "src/main/resources/assets/skillcraft/textures/entity/equipment/";
    static final String EFFECT = "src/main/resources/assets/skillcraft/textures/mob_effect/";

    public static void main(String[] args) throws Exception {
        new File(ITEMS).mkdirs();
        new File(EQUIP + "humanoid").mkdirs();
        new File(EQUIP + "humanoid_leggings").mkdirs();
        new File(EFFECT).mkdirs();

        // Warhammers: metal palettes (outline, dark, mid, light, shine)
        warhammer("copper_warhammer", new int[]{0x3A1E10, 0x8C4A26, 0xC06A3A, 0xE08A55, 0xF8C49A});
        warhammer("iron_warhammer", new int[]{0x2A2A2E, 0x6E7178, 0xA3A7AE, 0xCDD1D6, 0xF2F4F6});
        warhammer("golden_warhammer", new int[]{0x4A3208, 0xB07A12, 0xE0B020, 0xF6D850, 0xFFF4B0});
        warhammer("diamond_warhammer", new int[]{0x0E3A3A, 0x1E8C88, 0x33C9C0, 0x7AF0E4, 0xD8FFFA});
        warhammer("netherite_warhammer", new int[]{0x16121A, 0x3A3238, 0x4E444C, 0x6A5E66, 0x8E8088});

        sprite("light_arrow", LIGHT_ARROW, Map.of('o', 0x2E2416, 'w', 0x8A6A44, 'W', 0xB89060, 'f', 0xF0F0EA, 'F', 0xBFC4C8, 't', 0x9A9690, 'T', 0xD4D0C8));
        sprite("heavy_arrow", HEAVY_ARROW, Map.of('o', 0x1E1A16, 'w', 0x5E4228, 'W', 0x7E5A36, 'f', 0x4A4A52, 'F', 0x6E6E78, 't', 0x5A5E66, 'T', 0xA8AEB6, 'S', 0xE6EAEE));

        Map<Character, Integer> wool = Map.of('o', 0x3B2A1A, 'a', 0xC9B48A, 'b', 0xA8946C, 'c', 0xE3D3AD, 's', 0x6E5534, 'l', 0x7A4F2C, 'L', 0x9A6A3E);
        sprite("padded_helmet", PADDED_HELMET, wool);
        sprite("padded_chestplate", PADDED_CHEST, wool);
        sprite("padded_leggings", PADDED_LEGS, wool);
        sprite("padded_boots", PADDED_BOOTS, wool);
        paddedLayers();

        sprite("ember_sigil", EMBER_SIGIL, Map.of('o', 0x1A1210, 'd', 0x3A2C26, 'm', 0x5C4A40, 'e', 0xE0601A, 'E', 0xFFB040, 'y', 0xFFF0A0, 'g', 0xC29A48));
        sprite("slag_glob", SLAG_GLOB, Map.of('o', 0x1A0E0A, 'd', 0x4A2416, 'e', 0xC2410C, 'E', 0xFF8A1E, 'y', 0xFFE07A));
        sprite("emberfall_maul", MAUL, Map.of('o', 0x0E0C0C, 'd', 0x2C2A2E, 'm', 0x48464C, 'l', 0x6C6A72, 'e', 0xE0601A, 'E', 0xFFB040, 'w', 0x3E2A1C, 'W', 0x5E4430, 'g', 0xC29A48));
        sprite("summoning_brazier", BRAZIER_ITEM, Map.of('o', 0x141214, 'd', 0x2C282C, 'm', 0x4A444A, 'i', 0x8C9096, 'e', 0xE0601A, 'E', 0xFFB040, 'y', 0xFFF0A0));
        image(EFFECT + "sundered.png", grid(SUNDERED, Map.of('o', 0x1A1A1E, 'm', 0x8C9096, 'l', 0xC8CCD2, 'r', 0xE04020, 'R', 0xFF8A40)));
        System.out.println("Wrote combat textures.");
    }

    // --- Warhammer: a 45-degree handle with a square head across its top end ---
    static void warhammer(String name, int[] p) throws Exception {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        int wood = 0x6B4A2E, woodDark = 0x3E2A18, grip = 0x2E2A2A;
        // Handle along x + y = 15, from (1,14) up to (9,6)
        for (int x = 1; x <= 9; x++) {
            int y = 15 - x;
            px(img, x, y, x <= 3 ? grip : wood);
            px(img, x + 1, y, woodDark);
            px(img, x, y + 1, woodDark);
        }
        // Head: centred on (11,4), long axis along (1,1), short axis along the handle
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                double along = (x + y) - 15;      // across the handle
                double axis = (x - y) - 7;        // along the handle
                if (Math.abs(along) <= 5.5 && Math.abs(axis) <= 2.0) {
                    int c = p[2];
                    if (axis <= -1.5 || along <= -5) c = p[3];   // lit upper-left
                    if (axis >= 1.5 || along >= 5) c = p[1];     // shadowed lower-right
                    if (Math.abs(along) <= 1 && Math.abs(axis) <= 0.5) c = p[4]; // shine
                    px(img, x, y, c);
                }
            }
        }
        outline(img, p[0]);
        ImageIO.write(img, "png", new File(ITEMS + name + ".png"));
    }

    static void sprite(String name, String[] rows, Map<Character, Integer> palette) throws Exception {
        image(ITEMS + name + ".png", grid(rows, palette));
    }

    static BufferedImage grid(String[] rows, Map<Character, Integer> palette) {
        int h = rows.length, w = rows[0].length();
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            if (rows[y].length() != w) throw new IllegalStateException("Row " + y + " has " + rows[y].length() + " pixels, expected " + w);
            for (int x = 0; x < w; x++) {
                char ch = rows[y].charAt(x);
                if (ch == '.') continue;
                Integer c = palette.get(ch);
                if (c == null) throw new IllegalStateException("No colour for '" + ch + "'");
                px(img, x, y, c);
            }
        }
        return img;
    }

    static void image(String path, BufferedImage img) throws Exception {
        ImageIO.write(img, "png", new File(path));
    }

    // Adds a 1px outline around every opaque pixel that touches transparency
    static void outline(BufferedImage img, int color) {
        boolean[][] solid = new boolean[16][16];
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) solid[y][x] = (img.getRGB(x, y) >>> 24) != 0;
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
            if (solid[y][x]) continue;
            boolean touches = false;
            for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                int nx = x + d[0], ny = y + d[1];
                if (nx >= 0 && ny >= 0 && nx < 16 && ny < 16 && solid[ny][nx]) touches = true;
            }
            if (touches && !(x <= 3 && y >= 12)) px(img, x, y, color);
        }
    }

    static void px(BufferedImage img, int x, int y, int rgb) {
        if (x >= 0 && y >= 0 && x < img.getWidth() && y < img.getHeight()) img.setRGB(x, y, 0xFF000000 | rgb);
    }

    // --- Padded armour layers (64x32, same layout as vanilla armour) ---
    static void paddedLayers() throws Exception {
        BufferedImage body = new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB);
        BufferedImage legs = new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB);
        Random rng = new Random(7);
        // Head (cap): top + sides + back, front only across the brow
        quilt(body, 0, 0, 32, 16, rng);
        clear(body, 8, 8 + 4, 8, 4);          // front of the head below the brow
        clear(body, 40, 0, 24, 16);           // hat overlay unused
        // Body (gambeson) and arms (sleeves)
        quilt(body, 16, 16, 24, 16, rng);
        quilt(body, 40, 16, 16, 16, rng);
        // Leather belt across the body
        for (int x = 16; x < 40; x++) { px(body, x, 26, 0x7A4F2C); px(body, x, 27, 0x5A3820); }
        // Legs: boots in the body layer, trousers in the leggings layer
        quilt(body, 0, 16, 16, 16, rng);
        clear(body, 0, 16, 16, 9);            // only the lower leg (boot) part
        for (int x = 0; x < 16; x++) { px(body, x, 26, 0x7A4F2C); }
        quilt(legs, 0, 16, 16, 16, rng);
        quilt(legs, 16, 16, 24, 16, rng);
        clear(legs, 16, 16, 24, 4);           // leggings cover the waist band only
        for (int x = 16; x < 40; x++) px(legs, x, 26, 0x7A4F2C);
        image(EQUIP + "humanoid/padded.png", body);
        image(EQUIP + "humanoid_leggings/padded.png", legs);
    }

    // Quilted wool: diamond stitching over a softly noisy cream base
    static void quilt(BufferedImage img, int x0, int y0, int w, int h, Random rng) {
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            int gx = x0 + x, gy = y0 + y;
            boolean stitch = ((gx + gy) % 4 == 0) || ((gx - gy + 64) % 4 == 0);
            int base = rng.nextInt(5) == 0 ? 0xBBA67C : 0xC9B48A;
            px(img, gx, gy, stitch ? 0x8A7250 : base);
        }
    }

    static void clear(BufferedImage img, int x0, int y0, int w, int h) {
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) img.setRGB(x0 + x, y0 + y, 0);
    }

    // ---------------- Sprites ----------------
    static final String[] LIGHT_ARROW = {
            "................",
            "...........oTo..",
            "..........oTto..",
            ".........oTto...",
            "........oWoo....",
            ".......oWo......",
            "......oWo.......",
            ".....oWo........",
            "....oWo.........",
            "...oWo..........",
            "..oWo...........",
            ".ofWo...........",
            "offFo...........",
            ".ofo............",
            "..o.............",
            "................",
    };
    static final String[] HEAVY_ARROW = {
            "..........oooo..",
            ".........oSTTo..",
            "........oTTtto..",
            "........oTttoo..",
            ".......oWotto...",
            "......oWwoo.....",
            ".....oWwo.......",
            "....oWwo........",
            "...oWwo.........",
            "..oWwo..........",
            ".offo...........",
            "offFfo..........",
            "ofFFo...........",
            "offo............",
            ".oo.............",
            "................",
    };
    static final String[] PADDED_HELMET = {
            "................",
            "................",
            "................",
            ".....oooooo.....",
            "....occaaaao....",
            "...ocasaasaao...",
            "...oaasaasabo...",
            "...oasaasaabo...",
            "...oaaaaaabbo...",
            "...ollllllllo...",
            "...olo....olo...",
            "...oao....obo...",
            "...ooo....ooo...",
            "................",
            "................",
            "................",
    };
    static final String[] PADDED_CHEST = {
            "................",
            "..oooo....oooo..",
            ".occao....oaabo.",
            ".ocaaoooooobabo.",
            ".oasaasaasaasbo.",
            ".ooaasaasaasaoo.",
            "...oasaasaabo...",
            "...osaasaasbo...",
            "...oaasaasabo...",
            "...ollLllLllo...",
            "...oasaasaabo...",
            "...osaasaasbo...",
            "...oaasaasabo...",
            "...obbbbbbbbo...",
            "...oooooooooo...",
            "................",
    };
    static final String[] PADDED_LEGS = {
            "................",
            "...oooooooooo...",
            "...ollLllLllo...",
            "...oasaasaabo...",
            "...osaasaasbo...",
            "...oaasoosabo...",
            "...oasao.osbo...",
            "...osao..oabo...",
            "...oaso..osbo...",
            "...oaao..oabo...",
            "...osao..osbo...",
            "...oaso..oabo...",
            "...obbo..obbo...",
            "...oooo..oooo...",
            "................",
            "................",
    };
    static final String[] PADDED_BOOTS = {
            "................",
            "................",
            "................",
            "................",
            "................",
            "...oooo..oooo...",
            "...oaso..oaso...",
            "...osao..osao...",
            "...oaso..oaso...",
            "...olLo..olLo...",
            "..oaaso..oaasoo.",
            ".oaasaao.oasaabo",
            ".olllllo.ollllo.",
            ".ooooooo.oooooo.",
            "................",
            "................",
    };
    static final String[] EMBER_SIGIL = {
            "................",
            ".....oooooo.....",
            "....odmmmmdo....",
            "...odmggggmdo...",
            "..odmgeEEegmdo..",
            "..omgeEyyEegmo..",
            ".odmgEyEEyEgmdo.",
            ".omgeEEyyEEegmo.",
            ".omgeEEyyEEegmo.",
            ".odmgEyEEyEgmdo.",
            "..omgeEyyEegmo..",
            "..odmgeEEegmdo..",
            "...odmggggmdo...",
            "....odmmmmdo....",
            ".....oooooo.....",
            "................",
    };
    static final String[] SLAG_GLOB = {
            "................",
            "................",
            "................",
            "......ooo.......",
            "....oodeeoo.....",
            "...odeEEEedo....",
            "..odeEyyEEedo...",
            "..oeEyyEEEeeo...",
            "..odEEEEEeddo...",
            "...odeEEeddo....",
            "....oddedoo.....",
            ".....oooo.......",
            "................",
            "................",
            "................",
            "................",
    };
    static final String[] MAUL = {
            "........oooo....",
            ".......olllmo...",
            "......olmmmmdo..",
            ".....olmeEmmmdo.",
            "....olmmEEemmdo.",
            "....odmmmeEemdo.",
            ".....odmmmmmdo..",
            "......odggdddo..",
            ".....oWgo.ooo...",
            "....oWwo........",
            "...oWwo.........",
            "..oWwo..........",
            ".ogwo...........",
            "ogwo............",
            "owo.............",
            ".o..............",
    };
    static final String[] BRAZIER_ITEM = {
            "................",
            ".....y..E.......",
            "....yE.EyE..E...",
            "...EyEEyEEeE....",
            "..oeEyEEyEEeo...",
            "..oiiiiiiiiio...",
            "..odmmmmmmmdo...",
            "...odmmmmmdo....",
            "....odmmmdo.....",
            ".....odmdo......",
            ".....odmdo......",
            ".....odmdo......",
            "....odmmmdo.....",
            "...odmmmmmdo....",
            "...ooooooooo....",
            "................",
    };
    static final String[] SUNDERED = {
            "..................",
            "..................",
            "....oooooooooo....",
            "...ollllrllllmo...",
            "...olmmmrRmmmmo...",
            "...olmmrRmmmmmo...",
            "...olmmmrRmmmmo...",
            "...olmmmmrRmmmo...",
            "....olmmrRmmmo....",
            "....olmmmrmmmo....",
            ".....olmrRmmo.....",
            ".....olmmrmmo.....",
            "......olmrmo......",
            ".......olmo.......",
            "........oo........",
            "..................",
            "..................",
            "..................",
    };
}
