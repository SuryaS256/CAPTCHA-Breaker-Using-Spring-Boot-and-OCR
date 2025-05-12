package com.demo.controller;

import com.demo.DemoApplication;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

@RestController
@RequestMapping("/api/captcha")
public class CaptchaController {

    @PostMapping("/decode")
    public ResponseEntity<String> decodeCaptcha(@RequestParam("file") MultipartFile file) {
        try {
            // 1. Read image from uploaded file
            BufferedImage original = ImageIO.read(file.getInputStream());

            // 2. Clean image
            BufferedImage cleaned = DemoApplication.cleanImage(original);

            // 3. Upscale image
            Image scaledInstance = cleaned.getScaledInstance(cleaned.getWidth() * 4,
                    cleaned.getHeight() * 4, Image.SCALE_SMOOTH);
            BufferedImage scaled = new BufferedImage(cleaned.getWidth() * 4,
                    cleaned.getHeight() * 4, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = scaled.createGraphics();
            g2d.drawImage(scaledInstance, 0, 0, null);
            g2d.dispose();

            // 4. OCR
            Tesseract tesseract = new Tesseract();
            tesseract.setDatapath("/usr/share/tesseract-ocr/4.00/tessdata"); // change if different
            tesseract.setTessVariable("user_defined_dpi", "300");

            String rawResult = tesseract.doOCR(scaled);
            String finalText = DemoApplication.cleanResult(rawResult);

            // 5. Return response
            return ResponseEntity.ok(finalText);

        } catch (IOException | TesseractException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to process CAPTCHA: " + e.getMessage());
        }
    }
}
