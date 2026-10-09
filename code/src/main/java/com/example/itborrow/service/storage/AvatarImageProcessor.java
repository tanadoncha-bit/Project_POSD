package com.example.itborrow.service.storage;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.*;

import javax.imageio.ImageIO;

@Component
public class AvatarImageProcessor {
    public byte[] process(MultipartFile file) {
        if (file.isEmpty() || file.getSize() > 2 * 1024 * 1024)
            throw new IllegalArgumentException("Choose a JPG or PNG image up to 2 MB.");
        try (var input = file.getInputStream();
                var stream = ImageIO.createImageInputStream(input)) {
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
                    throw new IllegalArgumentException(
                            "Image dimensions must not exceed 4096 x 4096 pixels.");
                var source = reader.read(0);
                int side = Math.min(width, height);
                var image = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
                var graphics = image.createGraphics();
                try {
                    graphics.setRenderingHint(
                            RenderingHints.KEY_INTERPOLATION,
                            RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                    graphics.drawImage(
                            source,
                            0,
                            0,
                            256,
                            256,
                            (width - side) / 2,
                            (height - side) / 2,
                            (width + side) / 2,
                            (height + side) / 2,
                            null);
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
            throw new IllegalArgumentException(
                    "Unable to read this image. Choose another JPG or PNG.");
        }
    }
}
