package com.agroempresa.erp.reportes.dto;

import java.math.BigDecimal;

public record ResumenMovimientosInventario(
        long cantidadMovimientos,
        long unidades,
        BigDecimal valor
) {
}
