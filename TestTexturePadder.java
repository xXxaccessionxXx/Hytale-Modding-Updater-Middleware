import com.hytale.updater.agent.TexturePadder;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class TestTexturePadder {
    public static void main(String[] args) throws IOException {
        System.out.println("Starting TexturePadder test...");

        // 1. Create a dummy non-compliant image
        File testDir = new File("test_mods");
        testDir.mkdirs();
        File testImg = new File(testDir, "test_texture.png");

        BufferedImage img = new BufferedImage(64, 48, BufferedImage.TYPE_INT_ARGB);
        ImageIO.write(img, "png", testImg);

        System.out.println("Created test image with dimensions: " + img.getWidth() + "x" + img.getHeight());

        // 2. Run the padder
        TexturePadder.processModTextures(testDir.getAbsolutePath());

        // 3. Verify the new size
        BufferedImage paddedImg = ImageIO.read(testImg);
        System.out.println("Padded image dimensions: " + paddedImg.getWidth() + "x" + paddedImg.getHeight());

        if (paddedImg.getWidth() == 64 && paddedImg.getHeight() == 64) {
            System.out.println("SUCCESS: Image padded correctly!");
        } else {
            System.err.println("FAILED: Image dimensions are incorrect.");
            System.exit(1);
        }
        
        // Clean up
        testImg.delete();
        testDir.delete();
    }
}
