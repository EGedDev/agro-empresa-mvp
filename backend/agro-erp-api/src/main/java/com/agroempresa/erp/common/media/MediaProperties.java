package com.agroempresa.erp.common.media;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

@ConfigurationProperties(prefix = "app.media")
public record MediaProperties(
        Path uploadDir
) {

    public MediaProperties {
        if (uploadDir == null) {
            uploadDir = Path.of("uploads");
        }
    }
}
