package edu.pucmm.icc352.events.infrastructure.qr;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.GlobalHistogramBinarizer;
import com.google.zxing.common.HybridBinarizer;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class QrImageDecoder {
    private static final Map<DecodeHintType, Object> HINTS = buildHints();

    public String decode(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length == 0) {
            throw new IllegalArgumentException("La imagen QR es obligatoria.");
        }

        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (image == null) {
                throw new IllegalArgumentException("El archivo enviado no es una imagen compatible.");
            }

            String decoded = tryDecode(image);
            if (decoded != null) {
                return decoded;
            }

            BufferedImage enlargedImage = resize(image, Math.max(image.getWidth() * 2, 480));
            decoded = tryDecode(enlargedImage);
            if (decoded != null) {
                return decoded;
            }

            throw new IllegalArgumentException("La imagen no contiene un QR legible.");
        } catch (NotFoundException exception) {
            throw new IllegalArgumentException("La imagen no contiene un QR legible.", exception);
        } catch (IOException exception) {
            throw new IllegalArgumentException("No se pudo leer la imagen enviada.", exception);
        }
    }

    private String tryDecode(BufferedImage image) throws NotFoundException {
        BufferedImageLuminanceSource source = new BufferedImageLuminanceSource(image);
        List<BinaryBitmap> attempts = List.of(
                new BinaryBitmap(new HybridBinarizer(source)),
                new BinaryBitmap(new GlobalHistogramBinarizer(source)),
                new BinaryBitmap(new HybridBinarizer(source.invert())),
                new BinaryBitmap(new GlobalHistogramBinarizer(source.invert()))
        );

        NotFoundException lastException = null;
        for (BinaryBitmap bitmap : attempts) {
            try {
                return new MultiFormatReader().decode(bitmap, HINTS).getText();
            } catch (NotFoundException exception) {
                lastException = exception;
            }
        }

        if (lastException != null) {
            throw lastException;
        }

        return null;
    }

    private BufferedImage resize(BufferedImage image, int targetWidth) {
        int safeWidth = Math.max(targetWidth, image.getWidth());
        int targetHeight = Math.max(1, (int) Math.round((double) image.getHeight() * safeWidth / image.getWidth()));

        BufferedImage scaled = new BufferedImage(safeWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = scaled.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.drawImage(image, 0, 0, safeWidth, targetHeight, null);
        } finally {
            graphics.dispose();
        }
        return scaled;
    }

    private static Map<DecodeHintType, Object> buildHints() {
        Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
        hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
        hints.put(DecodeHintType.POSSIBLE_FORMATS, List.of(BarcodeFormat.QR_CODE));
        hints.put(DecodeHintType.CHARACTER_SET, "UTF-8");
        return hints;
    }
}
