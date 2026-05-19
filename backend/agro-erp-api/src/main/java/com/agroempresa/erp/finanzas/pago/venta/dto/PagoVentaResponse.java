package com.agroempresa.erp.finanzas.pago.venta.dto;

import com.agroempresa.erp.finanzas.MetodoPago;
import com.agroempresa.erp.finanzas.pago.venta.PagoVenta;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PagoVentaResponse(
        Long id,
        String numero,
        Long ventaId,
        BigDecimal monto,
        MetodoPago metodoPago,
        String referencia,
        LocalDateTime fechaPago,
        LocalDateTime creadoEn,
        boolean anulado,
        LocalDateTime fechaAnulacion,
        String motivoAnulacion
) {

    public static PagoVentaResponse desdeEntidad(PagoVenta pagoVenta) {
        return new PagoVentaResponse(
                pagoVenta.getId(),
                pagoVenta.getNumero(),
                pagoVenta.getVenta().getId(),
                pagoVenta.getMonto(),
                pagoVenta.getMetodoPago(),
                pagoVenta.getReferencia(),
                pagoVenta.getFechaPago(),
                pagoVenta.getCreadoEn(),
                pagoVenta.isAnulado(),
                pagoVenta.getFechaAnulacion(),
                pagoVenta.getMotivoAnulacion()
        );
    }
}
