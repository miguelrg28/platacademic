package edu.pucmm.icc352.events.infrastructure.qr;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class QrPayloadCodec {
    private final ObjectMapper objectMapper;

    public QrPayloadCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String encode(QrPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("No se pudo serializar el contenido del QR.", exception);
        }
    }

    public QrPayload decode(String content) {
        try {
            return objectMapper.readValue(content, QrPayload.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("El contenido del QR no es válido.", exception);
        }
    }
}
