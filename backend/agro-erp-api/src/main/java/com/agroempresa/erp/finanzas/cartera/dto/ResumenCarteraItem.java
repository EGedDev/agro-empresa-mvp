package com.agroempresa.erp.finanzas.cartera.dto;

import java.math.BigDecimal;

public record ResumenCarteraItem(
        long cantidadDocumentos,
        BigDecimal saldoPendiente,
        long cantidadVencida,
        BigDecimal saldoVencido,
        long cantidadPorVencer,
        BigDecimal saldoPorVencer
) {
}
