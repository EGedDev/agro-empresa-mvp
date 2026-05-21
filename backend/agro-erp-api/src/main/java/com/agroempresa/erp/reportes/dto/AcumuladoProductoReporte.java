package com.agroempresa.erp.reportes.dto;

import java.math.BigDecimal;

public record AcumuladoProductoReporte(
        Long productoId,
        String productoNombre,
        Long unidades,
        BigDecimal total
) {
}
