package com.agroempresa.erp.reportes.dto;

import java.math.BigDecimal;

public record ResumenOperacionesFinancieras(
        long cantidad,
        BigDecimal total,
        BigDecimal saldoPendiente
) {
}
