package com.agroempresa.erp.finanzas.caja.dto;

import com.agroempresa.erp.finanzas.MetodoPago;
import com.agroempresa.erp.finanzas.caja.CierreCajaMetodoPago;

import java.math.BigDecimal;

public record DiferenciaMetodoPagoCierreCajaResponse(
        MetodoPago metodoPago,
        long cantidadIngresos,
        long cantidadEgresos,
        BigDecimal totalIngresos,
        BigDecimal totalEgresos,
        BigDecimal saldoCalculado,
        BigDecimal saldoReportado,
        BigDecimal diferencia,
        boolean tieneDiferencia
) {

    public static DiferenciaMetodoPagoCierreCajaResponse desdeEntidad(CierreCajaMetodoPago metodo) {
        return new DiferenciaMetodoPagoCierreCajaResponse(
                metodo.getMetodoPago(),
                metodo.getCantidadIngresos(),
                metodo.getCantidadEgresos(),
                metodo.getTotalIngresos(),
                metodo.getTotalEgresos(),
                metodo.getSaldoCalculado(),
                metodo.getSaldoReportado(),
                metodo.getDiferencia(),
                metodo.getDiferencia().compareTo(BigDecimal.ZERO) != 0
        );
    }
}
