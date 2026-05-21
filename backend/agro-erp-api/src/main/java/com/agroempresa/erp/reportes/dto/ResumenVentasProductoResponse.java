package com.agroempresa.erp.reportes.dto;

import java.math.BigDecimal;

public record ResumenVentasProductoResponse(
        Long productoId,
        String productoNombre,
        long unidadesVendidas,
        long unidadesDevueltas,
        long unidadesNetas,
        BigDecimal totalVendido,
        BigDecimal totalDevuelto,
        BigDecimal totalNeto
) {
}
