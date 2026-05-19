package com.agroempresa.erp.finanzas.caja.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ResumenCajaResponse(
        LocalDate desde,
        LocalDate hasta,
        ResumenMovimientoCaja ingresos,
        ResumenMovimientoCaja egresos,
        BigDecimal saldoNeto,
        LocalDateTime generadoEn
) {
}
