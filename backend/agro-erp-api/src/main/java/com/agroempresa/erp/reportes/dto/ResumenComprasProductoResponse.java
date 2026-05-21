package com.agroempresa.erp.reportes.dto;

import java.math.BigDecimal;

public record ResumenComprasProductoResponse(
        Long productoId,
        String productoNombre,
        long unidadesCompradas,
        long unidadesDevueltas,
        long unidadesNetas,
        BigDecimal totalComprado,
        BigDecimal totalDevuelto,
        BigDecimal totalNeto
) {
}
