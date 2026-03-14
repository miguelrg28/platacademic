package edu.pucmm.icc352.events.infrastructure.qr;

public record QrPayload(
        Long eventId,
        Long userId,
        String token
) {
}
