package edu.pucmm.icc352.events.infrastructure.qr;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QrImageDecoderTest {
    @Test
    void shouldDecodeGeneratedQrImage() {
        QrCodeGenerator generator = new QrCodeGenerator();
        QrImageDecoder decoder = new QrImageDecoder();
        String content = "{\"eventId\":1,\"userId\":2,\"token\":\"demo-token\"}";

        byte[] image = generator.generatePng(content);

        String decoded = decoder.decode(image);

        assertEquals(content, decoded);
    }

    @Test
    void shouldDecodeQrEmbeddedInsideLargerPng() throws IOException {
        QrCodeGenerator generator = new QrCodeGenerator();
        QrImageDecoder decoder = new QrImageDecoder();
        String content = "{\"eventId\":9,\"userId\":7,\"token\":\"screen-shot-demo\"}";

        byte[] qrImage = generator.generatePng(content);
        byte[] screenshotLikeImage = embedIntoLargeCanvas(qrImage);

        String decoded = decoder.decode(screenshotLikeImage);

        assertEquals(content, decoded);
    }

    @Test
    void shouldRejectImageWithoutReadableQr() {
        QrImageDecoder decoder = new QrImageDecoder();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> decoder.decode("not-an-image".getBytes())
        );

        assertEquals("El archivo enviado no es una imagen compatible.", exception.getMessage());
    }

    private byte[] embedIntoLargeCanvas(byte[] qrImage) throws IOException {
        BufferedImage original = ImageIO.read(new ByteArrayInputStream(qrImage));
        BufferedImage canvas = new BufferedImage(1200, 900, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = canvas.createGraphics();

        try {
            graphics.setColor(new Color(250, 246, 239));
            graphics.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
            graphics.setColor(new Color(20, 34, 48));
            graphics.fillRoundRect(140, 100, 920, 700, 28, 28);
            graphics.setColor(new Color(255, 252, 246));
            graphics.fillRoundRect(190, 160, 820, 580, 22, 22);
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.drawImage(original, 380, 245, 240, 240, null);
        } finally {
            graphics.dispose();
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(canvas, "png", output);
        return output.toByteArray();
    }
}
