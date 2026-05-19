package com.agroempresa.erp.finanzas.caja.dto;

import com.agroempresa.erp.finanzas.MetodoPago;
import com.agroempresa.erp.finanzas.caja.CierreCajaMetodoPago;

import java.math.BigDecimal;

public record CierreCajaMetodoPagoResponse(
        MetodoPago metodoPago,
        long cantidadIngresos,
        long cantidadEgresos,
        BigDecimal totalIngresos,
        BigDecimal totalEgresos,
        BigDecimal saldoCalculado,
        BigDecimal saldoReportado,
        BigDecimal diferencia
) {

    public static CierreCajaMetodoPagoResponse desdeEntidad(CierreCajaMetodoPago metodo) {
        return new CierreCajaMetodoPagoResponse(
                metodo.getMetodoPago(),
                metodo.getCantidadIngresos(),
                metodo.getCantidadEgresos(),
                metodo.getTotalIngresos(),
                metodo.getTotalEgresos(),
                metodo.getSaldoCalculado(),
                metodo.getSaldoReportado(),
                metodo.getDiferencia()
        );
    }
}
