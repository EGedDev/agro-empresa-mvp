package com.agroempresa.erp.seguridad;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(
        String secret,
        String issuer,
        long expirationMinutes
) {

    public JwtProperties {
        if (secret == null || secret.length() < 32) {
            throw new IllegalArgumentException("JWT_SECRET debe tener al menos 32 caracteres");
        }

        if (issuer == null || issuer.isBlank()) {
            issuer = "agro-erp-api";
        }

        if (expirationMinutes <= 0) {
            expirationMinutes = 480;
        }
    }

    public Duration expiration() {
        return Duration.ofMinutes(expirationMinutes);
    }
}
