package com.agroempresa.erp.reportes.dto;

import java.math.BigDecimal;

public record AcumuladoRentabilidadProducto(
        Long productoId,
        String productoNombre,
        Long unidades,
        BigDecimal ingresos,
        BigDecimal costoVentas
) {
}
