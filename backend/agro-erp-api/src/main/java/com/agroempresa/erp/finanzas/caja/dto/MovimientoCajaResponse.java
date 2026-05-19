package com.agroempresa.erp.finanzas.caja.dto;

import com.agroempresa.erp.finanzas.MetodoPago;
import com.agroempresa.erp.finanzas.caja.MovimientoCaja;
import com.agroempresa.erp.finanzas.caja.TipoMovimientoCaja;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MovimientoCajaResponse(
        Long id,
        TipoMovimientoCaja tipo,
        BigDecimal monto,
        MetodoPago metodoPago,
        String referencia,
        String referenciaTipo,
        Long referenciaId,
        LocalDateTime fechaMovimiento,
        LocalDateTime creadoEn
) {

    public static MovimientoCajaResponse desdeEntidad(MovimientoCaja movimientoCaja) {
        return new MovimientoCajaResponse(
                movimientoCaja.getId(),
                movimientoCaja.getTipo(),
                movimientoCaja.getMonto(),
                movimientoCaja.getMetodoPago(),
                movimientoCaja.getReferencia(),
                movimientoCaja.getReferenciaTipo(),
                movimientoCaja.getReferenciaId(),
                movimientoCaja.getFechaMovimiento(),
                movimientoCaja.getCreadoEn()
        );
    }
}
