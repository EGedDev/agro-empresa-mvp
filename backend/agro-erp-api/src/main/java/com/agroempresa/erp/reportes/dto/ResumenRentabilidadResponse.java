package com.agroempresa.erp.reportes.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ResumenRentabilidadResponse(
        LocalDate desde,
        LocalDate hasta,
        long ventas,
        BigDecimal ingresosBrutos,
        BigDecimal costoVentasBruto,
        BigDecimal devolucionesVenta,
        BigDecimal costoDevuelto,
        BigDecimal ingresosNetos,
        BigDecimal costoVentasNeto,
        BigDecimal utilidadBruta,
        BigDecimal margenBrutoPorcentaje,
        LocalDateTime generadoEn
) {
}
