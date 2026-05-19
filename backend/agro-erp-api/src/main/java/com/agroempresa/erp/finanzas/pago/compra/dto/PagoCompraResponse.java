package com.agroempresa.erp.finanzas.pago.compra.dto;

import com.agroempresa.erp.finanzas.MetodoPago;
import com.agroempresa.erp.finanzas.pago.compra.PagoCompra;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PagoCompraResponse(
        Long id,
        String numero,
        Long compraId,
        BigDecimal monto,
        MetodoPago metodoPago,
        String referencia,
        LocalDateTime fechaPago,
        LocalDateTime creadoEn,
        boolean anulado,
        LocalDateTime fechaAnulacion,
        String motivoAnulacion
) {

    public static PagoCompraResponse desdeEntidad(PagoCompra pagoCompra) {
        return new PagoCompraResponse(
                pagoCompra.getId(),
                pagoCompra.getNumero(),
                pagoCompra.getCompra().getId(),
                pagoCompra.getMonto(),
                pagoCompra.getMetodoPago(),
                pagoCompra.getReferencia(),
                pagoCompra.getFechaPago(),
                pagoCompra.getCreadoEn(),
                pagoCompra.isAnulado(),
                pagoCompra.getFechaAnulacion(),
                pagoCompra.getMotivoAnulacion()
        );
    }
}
