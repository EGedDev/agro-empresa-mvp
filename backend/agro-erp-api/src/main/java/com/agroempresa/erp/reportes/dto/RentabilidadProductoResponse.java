package com.agroempresa.erp.reportes.dto;

import java.math.BigDecimal;

public record RentabilidadProductoResponse(
        Long productoId,
        String productoNombre,
        long unidadesVendidas,
        long unidadesDevueltas,
        long unidadesNetas,
        BigDecimal ingresosNetos,
        BigDecimal costoVentasNeto,
        BigDecimal utilidadBruta,
        BigDecimal margenBrutoPorcentaje
) {
}
