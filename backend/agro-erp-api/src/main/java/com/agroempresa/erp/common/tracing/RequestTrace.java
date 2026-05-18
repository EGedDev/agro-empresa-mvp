package com.agroempresa.erp.common.tracing;

public record RequestTrace(
        String correlationId,
        String ipAddress,
        String userAgent
) {
}
