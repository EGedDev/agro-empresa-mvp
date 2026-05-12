package com.agroempresa.erp.common.health;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    @GetMapping
    public HealthResponse check() {
        return new HealthResponse(
                "OK",
                "Agro ERP API funcionando correctamente",
                Instant.now()
        );
    }

    public record HealthResponse(
            String status,
            String message,
            Instant timestamp
    ) {
    }
}