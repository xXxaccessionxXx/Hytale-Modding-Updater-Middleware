package com.hytale.updater.agent;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class TexturePadder {

    /**
     * Recursively scans a directory for .png files and pads their dimensions to the nearest multiple of 32.
     *
     * @param modsDirectory The absolute path to the mods directory.
     * @return the number of textures that were successfully padded.
     */
    public static int processModTextures(String modsDirectory) {
        int[] paddedCount = {0};
        System.out.println("[Hytale Middleware] Starting Texture Padder in: " + modsDirectory);
        Path startPath = Paths.get(modsDirectory);

        if (!Files.exists(startPath) || !Files.isDirectory(startPath)) {
            System.err.println("[Hytale Middleware] Invalid mods directory for texture padding: " + modsDirectory);
            return 0;
        }

        try {
            Files.walkFileTree(startPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (file.toString().toLowerCase().endsWith(".png")) {
                        if (padImageIfNeeded(file.toFile())) {
                            paddedCount[0]++;
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
            System.out.println("[Hytale Middleware] Texture padding complete. Padded " + paddedCount[0] + " textures.");
        } catch (IOException e) {
            System.err.println("[Hytale Middleware] Error while walking mods directory: " + e.getMessage());
            e.printStackTrace();
        }
        return paddedCount[0];
    }

    private static boolean padImageIfNeeded(File imageFile) {
        try {
            BufferedImage originalImage = ImageIO.read(imageFile);
            if (originalImage == null) {
                // Not a valid image or unrecognized format
                return false;
            }

            int width = originalImage.getWidth();
            int height = originalImage.getHeight();

            boolean needsPadding = false;
            int newWidth = width;
            int newHeight = height;

            if (width < 32 || width % 32 != 0) {
                newWidth = (int) (Math.ceil(width / 32.0) * 32);
                if (newWidth == 0) newWidth = 32;
                needsPadding = true;
            }

            if (height < 32 || height % 32 != 0) {
                newHeight = (int) (Math.ceil(height / 32.0) * 32);
                if (newHeight == 0) newHeight = 32;
                needsPadding = true;
            }

            if (needsPadding) {
                System.out.println("[TexturePadder] Padding " + imageFile.getName() + " from " + width + "x" + height + " to " + newWidth + "x" + newHeight);
                
                // Create a new image with the correct size and transparency
                BufferedImage paddedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2d = paddedImage.createGraphics();
                
                // Ensure transparent background (usually default for ARGB, but good to be explicit)
                g2d.setComposite(AlphaComposite.Clear);
                g2d.fillRect(0, 0, newWidth, newHeight);
                g2d.setComposite(AlphaComposite.SrcOver);

                // Draw the original image at (0, 0)
                g2d.drawImage(originalImage, 0, 0, null);
                g2d.dispose();

                // Overwrite the original file
                ImageIO.write(paddedImage, "png", imageFile);
                return true;
            }
            return false;

        } catch (IOException e) {
            System.err.println("[TexturePadder] Error trying to pad image " + imageFile.getAbsolutePath() + ": " + e.getMessage());
        }
        return false;
    }
}
