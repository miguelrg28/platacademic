package edu.pucmm.icc352.events.config;

public record AdminSettings(
        String username,
        String password,
        String fullName,
        String email
) {
}
