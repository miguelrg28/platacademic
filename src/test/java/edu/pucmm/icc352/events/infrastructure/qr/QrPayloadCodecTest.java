package edu.pucmm.icc352.events.infrastructure.qr;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QrPayloadCodecTest {
    @Test
    void shouldEncodeAndDecodePayload() {
        QrPayloadCodec codec = new QrPayloadCodec(new ObjectMapper());
        QrPayload payload = new QrPayload(12L, 34L, "abc-token");

        String encoded = codec.encode(payload);
        QrPayload decoded = codec.decode(encoded);

        assertEquals(payload, decoded);
    }
}
