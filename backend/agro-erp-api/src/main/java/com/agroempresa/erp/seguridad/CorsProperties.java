package com.agroempresa.erp.seguridad;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.security.cors")
public record CorsProperties(
        List<String> allowedOrigins
) {

    private static final List<String> DEFAULT_ALLOWED_ORIGINS = List.of(
            "http://localhost:5173",
            "http://127.0.0.1:5173",
            "http://localhost:3000",
            "http://127.0.0.1:3000"
    );

    public CorsProperties {
        if (allowedOrigins == null || allowedOrigins.isEmpty()) {
            allowedOrigins = DEFAULT_ALLOWED_ORIGINS;
        }

        allowedOrigins = allowedOrigins.stream()
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .distinct()
                .toList();
    }
}
