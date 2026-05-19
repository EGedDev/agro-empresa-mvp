package com.agroempresa.erp.finanzas.cartera.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ResumenCarteraResponse(
        LocalDate desde,
        LocalDate hasta,
        ResumenCarteraItem cuentasPorCobrar,
        ResumenCarteraItem cuentasPorPagar,
        BigDecimal saldoNeto,
        LocalDateTime generadoEn
) {
}
