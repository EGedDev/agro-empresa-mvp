package com.agroempresa.erp.reportes.dto;

import java.math.BigDecimal;

public record ResumenVentasClienteResponse(
        Long clienteId,
        String clienteNombre,
        Long ventas,
        BigDecimal total,
        BigDecimal saldoPendiente
) {
}
