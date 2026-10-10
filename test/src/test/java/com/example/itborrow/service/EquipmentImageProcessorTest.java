package com.example.itborrow.service;

import static org.assertj.core.api.Assertions.*;

import com.example.itborrow.service.storage.EquipmentImageProcessor;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.awt.image.BufferedImage;
import java.io.*;

import javax.imageio.ImageIO;

class EquipmentImageProcessorTest {
    @Test
    void preservesAspectRatioAndRejectsInvalidContent() throws Exception {
        var output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(1600, 800, BufferedImage.TYPE_INT_RGB), "png", output);
        var processor = new EquipmentImageProcessor();
        var result =
                ImageIO.read(
                        new ByteArrayInputStream(
                                processor.process(
                                        new MockMultipartFile(
                                                "image",
                                                "test.png",
                                                "image/png",
                                                output.toByteArray()))));
        assertThat(result.getWidth()).isEqualTo(1200);
        assertThat(result.getHeight()).isEqualTo(600);
        assertThatThrownBy(
                        () ->
                                processor.process(
                                        new MockMultipartFile(
                                                "image",
                                                "fake.png",
                                                "image/png",
                                                new byte[] {1, 2, 3})))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
