package com.example.itborrow.service.storage;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import java.io.*;
import java.awt.image.BufferedImage;

@Component
public class EquipmentImageProcessor {
    public byte[] process(MultipartFile file) {
        if (file.isEmpty() || file.getSize() > 2 * 1024 * 1024)
            throw new IllegalArgumentException("Choose a JPG or PNG image up to 2 MB.");
        try (var input = file.getInputStream(); var stream = ImageIO.createImageInputStream(input)) {
            var readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext())
                throw new IllegalArgumentException("The file is not a supported image.");
            var reader = readers.next();
            try {
                String format = reader.getFormatName();
                if (!format.equalsIgnoreCase("JPEG") && !format.equalsIgnoreCase("PNG"))
                    throw new IllegalArgumentException("Only JPG and PNG images are supported.");
                reader.setInput(stream);
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (width < 1 || height < 1 || width > 4096 || height > 4096)
                    throw new IllegalArgumentException("Image dimensions must not exceed 4096 x 4096 pixels.");
                var source = reader.read(0);
                double scale = Math.min(1d, 1200d / Math.max(width, height));
                int targetWidth = Math.max(1, (int) (width * scale)),
                        targetHeight = Math.max(1, (int) (height * scale));
                var image = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
                var graphics = image.createGraphics();
                try {
                    graphics.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                            java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                    graphics.drawImage(source, 0, 0, targetWidth, targetHeight, null);
                } finally {
                    graphics.dispose();
                }
                var output = new ByteArrayOutputStream();
                ImageIO.write(image, "png", output);
                return output.toByteArray();
            } finally {
                reader.dispose();
            }
        } catch (IOException ex) {
            throw new IllegalArgumentException("Unable to read this image. Choose another JPG or PNG.");
        }
    }
}
