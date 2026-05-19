package com.agroempresa.erp.finanzas.caja.dto;

import com.agroempresa.erp.finanzas.caja.CierreCaja;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record CierreCajaResponse(
        Long id,
        LocalDate desde,
        LocalDate hasta,
        long cantidadIngresos,
        long cantidadEgresos,
        BigDecimal totalIngresos,
        BigDecimal totalEgresos,
        BigDecimal saldoCalculado,
        BigDecimal saldoReportado,
        BigDecimal diferencia,
        String observaciones,
        String cerradoPor,
        LocalDateTime creadoEn,
        List<CierreCajaMetodoPagoResponse> metodos
) {

    public static CierreCajaResponse desdeEntidad(CierreCaja cierreCaja) {
        return new CierreCajaResponse(
                cierreCaja.getId(),
                cierreCaja.getFechaDesde(),
                cierreCaja.getFechaHasta(),
                cierreCaja.getCantidadIngresos(),
                cierreCaja.getCantidadEgresos(),
                cierreCaja.getTotalIngresos(),
                cierreCaja.getTotalEgresos(),
                cierreCaja.getSaldoCalculado(),
                cierreCaja.getSaldoReportado(),
                cierreCaja.getDiferencia(),
                cierreCaja.getObservaciones(),
                cierreCaja.getCerradoPor(),
                cierreCaja.getCreadoEn(),
                cierreCaja.getMetodos().stream()
                        .map(CierreCajaMetodoPagoResponse::desdeEntidad)
                        .toList()
        );
    }
}
