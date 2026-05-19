package com.agroempresa.erp.reportes.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ResumenFinancieroResponse(
        LocalDate desde,
        LocalDate hasta,
        ResumenOperacionesFinancieras ventas,
        ResumenOperacionesFinancieras compras,
        BigDecimal devolucionesVenta,
        BigDecimal devolucionesCompra,
        BigDecimal cobrosRecibidos,
        BigDecimal pagosRealizados,
        BigDecimal flujoCajaNeto,
        LocalDateTime generadoEn
) {
}
