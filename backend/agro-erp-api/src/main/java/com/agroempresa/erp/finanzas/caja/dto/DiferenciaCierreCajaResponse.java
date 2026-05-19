package com.agroempresa.erp.finanzas.caja.dto;

import com.agroempresa.erp.finanzas.MetodoPago;
import com.agroempresa.erp.finanzas.caja.CierreCaja;
import com.agroempresa.erp.finanzas.caja.CierreCajaMetodoPago;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record DiferenciaCierreCajaResponse(
        Long cierreId,
        LocalDate desde,
        LocalDate hasta,
        BigDecimal saldoCalculado,
        BigDecimal saldoReportado,
        BigDecimal diferencia,
        boolean tieneDiferencia,
        String cerradoPor,
        LocalDateTime creadoEn,
        List<DiferenciaMetodoPagoCierreCajaResponse> metodos
) {

    public static DiferenciaCierreCajaResponse desdeEntidad(
            CierreCaja cierreCaja,
            MetodoPago metodoPago,
            boolean soloConDiferencia
    ) {
        return new DiferenciaCierreCajaResponse(
                cierreCaja.getId(),
                cierreCaja.getFechaDesde(),
                cierreCaja.getFechaHasta(),
                cierreCaja.getSaldoCalculado(),
                cierreCaja.getSaldoReportado(),
                cierreCaja.getDiferencia(),
                cierreCaja.getDiferencia().compareTo(BigDecimal.ZERO) != 0,
                cierreCaja.getCerradoPor(),
                cierreCaja.getCreadoEn(),
                cierreCaja.getMetodos().stream()
                        .filter(metodo -> coincideMetodo(metodo, metodoPago))
                        .filter(metodo -> !soloConDiferencia || metodo.getDiferencia().compareTo(BigDecimal.ZERO) != 0)
                        .map(DiferenciaMetodoPagoCierreCajaResponse::desdeEntidad)
                        .toList()
        );
    }

    private static boolean coincideMetodo(CierreCajaMetodoPago metodo, MetodoPago metodoPago) {
        return metodoPago == null || metodo.getMetodoPago() == metodoPago;
    }
}
