package com.agroempresa.erp.finanzas.pago.compra.dto;

import com.agroempresa.erp.finanzas.MetodoPago;
import com.agroempresa.erp.finanzas.pago.compra.PagoCompra;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PagoCompraResponse(
        Long id,
        Long compraId,
        BigDecimal monto,
        MetodoPago metodoPago,
        String referencia,
        LocalDateTime fechaPago,
        LocalDateTime creadoEn
) {

    public static PagoCompraResponse desdeEntidad(PagoCompra pagoCompra) {
        return new PagoCompraResponse(
                pagoCompra.getId(),
                pagoCompra.getCompra().getId(),
                pagoCompra.getMonto(),
                pagoCompra.getMetodoPago(),
                pagoCompra.getReferencia(),
                pagoCompra.getFechaPago(),
                pagoCompra.getCreadoEn()
        );
    }
}
