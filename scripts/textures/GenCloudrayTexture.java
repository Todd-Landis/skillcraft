import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Random;
import javax.imageio.ImageIO;

// Generates the Cloudray's textures (128 x 64):
//   textures/entity/cloudray/cloudray.png       - twilight-indigo back, pale cloud belly, speckles
//   textures/entity/cloudray/cloudray_glow.png  - storm-blue markings that glow (emissive layer)
// The CUBES table MUST match client/bonding/CloudrayModel (texOffs and box sizes).
// Usage: java scripts/textures/GenCloudrayTexture.java   (from the project root)
public class GenCloudrayTexture {
    static final String OUT = "src/main/resources/assets/skillcraft/textures/entity/cloudray/";
    static final int W = 128, H = 64;

    // name, u, v, w, h, d
    static final Object[][] CUBES = {
            {"body", 0, 0, 16, 4, 20},
            {"head", 0, 24, 10, 3, 6},
            {"horn", 32, 24, 2, 2, 5},
            {"wing", 0, 34, 22, 1, 16},
            {"tail", 80, 0, 2, 2, 18},
            {"tail_fin", 80, 22, 6, 1, 3},
    };

    static final int BACK = 0x2B3A67, BACK_LIGHT = 0x3E5490, EDGE = 0x4A5F91, BELLY = 0xD9E4F2, BELLY_SHADE = 0xB8C8DE, MARK = 0x7FD8FF;

    static BufferedImage base = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
    static BufferedImage glow = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
    static Random random = new Random(62);

    public static void main(String[] args) throws Exception {
        for (Object[] cube : CUBES) paint((String) cube[0], (int) cube[1], (int) cube[2], (int) cube[3], (int) cube[4], (int) cube[5]);
        new File(OUT).mkdirs();
        ImageIO.write(base, "png", new File(OUT + "cloudray.png"));
        ImageIO.write(glow, "png", new File(OUT + "cloudray_glow.png"));
        System.out.println("Wrote " + OUT + "cloudray.png and cloudray_glow.png");
    }

    // Standard box UV layout: top and bottom faces in the first row, then the four sides
    static void paint(String name, int u, int v, int w, int h, int d) {
        face(name, "top", u + d, v, w, d);
        face(name, "bottom", u + d + w, v, w, d);
        face(name, "side", u, v + d, d, h);
        face(name, "front", u + d, v + d, w, h);
        face(name, "side", u + d + w, v + d, d, h);
        face(name, "back", u + 2 * d + w, v + d, w, h);
    }

    static void face(String cube, String face, int x0, int y0, int w, int h) {
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                double fx = w <= 1 ? 0.5 : x / (double) (w - 1);
                double fy = h <= 1 ? 0.5 : y / (double) (h - 1);
                int color;
                switch (face) {
                    case "top" -> {
                        // Indigo back, lighter toward the edges (wing tips), with pale speckles
                        double edge = Math.abs(fx - 0.5) * 2;
                        color = mix(BACK, BACK_LIGHT, edge * 0.8);
                        if (random.nextInt(23) == 0) color = mix(color, BELLY, 0.5);
                    }
                    case "bottom" -> color = mix(BELLY, BELLY_SHADE, Math.abs(fx - 0.5) + 0.3 * random.nextDouble());
                    default -> color = mix(EDGE, BACK, fy * 0.6);
                }
                color = shade(color, 0.94 + 0.08 * random.nextDouble());
                base.setRGB(x0 + x, y0 + y, 0xFF000000 | color);
                if (face.equals("top") && marked(cube, fx, fy, w, h)) glow.setRGB(x0 + x, y0 + y, 0xFF000000 | MARK);
            }
        }
    }

    // Where the glowing markings sit on each cube's top face
    static boolean marked(String cube, double fx, double fy, int w, int h) {
        return switch (cube) {
            // A spine stripe down the middle, and three chevrons
            case "body" -> Math.abs(fx - 0.5) < 0.04 || chevron(fx, fy, 0.25) || chevron(fx, fy, 0.5) || chevron(fx, fy, 0.75);
            // Two long lines along each wing, and a dot near the tip
            case "wing" -> Math.abs(fy - 0.35) < 0.05 && fx > 0.15 && fx < 0.95
                    || Math.abs(fy - 0.65) < 0.05 && fx > 0.3 && fx < 0.85
                    || Math.hypot((fx - 0.9) * w, (fy - 0.5) * h) < 1.2;
            case "head" -> Math.abs(fy - 0.5) < 0.2 && (Math.abs(fx - 0.25) < 0.08 || Math.abs(fx - 0.75) < 0.08); // eyes
            case "tail" -> ((int) Math.round(fy * (h - 1))) % 4 == 0;
            default -> false;
        };
    }

    static boolean chevron(double fx, double fy, double at) {
        return Math.abs(fy - (at - Math.abs(fx - 0.5) * 0.3)) < 0.03 && Math.abs(fx - 0.5) < 0.35;
    }

    static int mix(int a, int b, double t) {
        t = Math.max(0, Math.min(1, t));
        int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return r << 16 | g << 8 | bl;
    }

    static int shade(int c, double f) {
        int r = Math.min(255, (int) (((c >> 16) & 255) * f));
        int g = Math.min(255, (int) (((c >> 8) & 255) * f));
        int b = Math.min(255, (int) ((c & 255) * f));
        return r << 16 | g << 8 | b;
    }
}
