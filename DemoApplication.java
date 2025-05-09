package com.demo;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class DemoApplication {
    static final int DELTA = 3;

    // Step 1: Check if a pixel is part of real content or noise
    public static boolean isEligible(BufferedImage img, int x, int y) {
        int left = x - 1;
        while (left >= 0 && x - left < 2 * DELTA) {
            if (img.getRGB(left, y) == Color.WHITE.getRGB()) break;
            left--;
        }
        if (left < 0) return false;

        int right = x + 1;
        while (right < img.getWidth() && right - left < 2 * DELTA) {
            if (img.getRGB(right, y) == Color.WHITE.getRGB()) break;
            right++;
        }
        if (right >= img.getWidth()) return false;

        int top = y - 1;
        while (top >= 0 && y - top < 2 * DELTA) {
            if (img.getRGB(x, top) == Color.WHITE.getRGB()) break;
            top--;
        }
        if (top < 0) return false;

        int bottom = y + 1;
        while (bottom < img.getHeight() && bottom - top < 2 * DELTA) {
            if (img.getRGB(x, bottom) == Color.WHITE.getRGB()) break;
            bottom++;
        }
        if (bottom >= img.getHeight()) return false;

        int width = right - left;
        int height = bottom - top;
        return width >= DELTA && height >= DELTA;
    }

    // Step 2: Clean the image by removing noise
    public static BufferedImage cleanImage(BufferedImage source) {
        int width = source.getWidth();
        int height = source.getHeight();
        BufferedImage cleaned = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);

        Graphics2D g2d = cleaned.createGraphics();
        g2d.drawImage(source, 0, 0, null);
        g2d.dispose();

        // Scan pixel by pixel
        for (int x = 1; x < width - 1; x++) {
            for (int y = 1; y < height - 1; y++) {
                int pixel = cleaned.getRGB(x, y);
                if (pixel != Color.WHITE.getRGB()) {
                    int surroundingBlack = 0;

                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dy = -1; dy <= 1; dy++) {
                            if (dx == 0 && dy == 0) continue;
                            if (cleaned.getRGB(x + dx, y + dy) != Color.WHITE.getRGB()) {
                                surroundingBlack++;
                            }
                        }
                    }

                    // If isolated or weak pixel, turn white
                    if (surroundingBlack < 2) {
                        cleaned.setRGB(x, y, Color.WHITE.getRGB());
                    }
                }
            }
        }

        return cleaned;
    }


    // Step 3: Clean up OCR result
    public static String cleanResult(String result) {
        StringBuilder sb = new StringBuilder();
        for (char c : result.toCharArray()) {
            if (Character.isLetterOrDigit(c)) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static void main(String[] args) throws IOException, TesseractException {
        // Step 1: Load original image
        File imageFile = new File("/home/surya/Downloads/Captcha 3532/Done/2a9AfK.png");
        BufferedImage original = ImageIO.read(imageFile);
        ImageIO.write(original, "png", new File("/home/surya/Documents/step1_original_copy.png"));
        System.out.println("✅ Saved: step1_original_copy.png");

        // Step 2: Clean noise
        BufferedImage cleaned = cleanImage(original);
        ImageIO.write(cleaned, "png", new File("/home/surya/Documents/step2_cleaned.png"));
        System.out.println("✅ Saved: step2_cleaned.png");

        // Step 3: Upscale for better OCR (simulate DPI ~300)
        Image scaledInstance = cleaned.getScaledInstance(cleaned.getWidth() * 4, cleaned.getHeight() * 4, Image.SCALE_SMOOTH);
        BufferedImage scaled = new BufferedImage(cleaned.getWidth() * 4, cleaned.getHeight() * 4, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = scaled.createGraphics();
        g2d.drawImage(scaledInstance, 0, 0, null);
        g2d.dispose();

        ImageIO.write(scaled, "png", new File("/home/surya/Documents/step3_upscaled.png"));
        System.out.println("✅ Saved: step3_upscaled.png");

        // Step 4: Perform OCR
        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath("/usr/share/tesseract-ocr/4.00/tessdata");  // Adjust for your system
        tesseract.setTessVariable("user_defined_dpi", "300");

        String rawResult = tesseract.doOCR(scaled);
        String finalText = cleanResult(rawResult);

        System.out.println("\n📌 Raw OCR result   : " + rawResult);
        System.out.println("✅ Cleaned OCR text : " + finalText);
    }
}
