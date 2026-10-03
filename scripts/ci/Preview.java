import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Arrays;
import java.util.Base64;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;

// Prints each showcase screenshot as a small base64 JPEG line ("PREVIEW <name> <data>"), so the
// pictures can be read straight from a CI log when artifacts can't be downloaded.
//   java scripts/ci/Preview.java run/screenshots [width]
public class Preview {
    public static void main(String[] args) throws Exception {
        File dir = new File(args.length > 0 ? args[0] : "run/screenshots");
        int width = args.length > 1 ? Integer.parseInt(args[1]) : 640;
        File[] files = dir.listFiles((d, name) -> name.startsWith("showcase_") && name.endsWith(".png"));
        if (files == null) return;
        Arrays.sort(files);
        for (File file : files) {
            BufferedImage source = ImageIO.read(file);
            int height = source.getHeight() * width / source.getWidth();
            BufferedImage small = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = small.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(source, 0, 0, width, height, null);
            g.dispose();
            ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(0.85f);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (MemoryCacheImageOutputStream out = new MemoryCacheImageOutputStream(bytes)) {
                writer.setOutput(out);
                writer.write(null, new IIOImage(small, null, null), param);
            }
            writer.dispose();
            System.out.println("PREVIEW " + file.getName() + " " + Base64.getEncoder().encodeToString(bytes.toByteArray()));
        }
    }
}
