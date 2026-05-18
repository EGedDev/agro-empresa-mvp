package com.agroempresa.erp.finanzas.pago.venta.dto;

import com.agroempresa.erp.finanzas.MetodoPago;
import com.agroempresa.erp.finanzas.pago.venta.PagoVenta;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PagoVentaResponse(
        Long id,
        Long ventaId,
        BigDecimal monto,
        MetodoPago metodoPago,
        String referencia,
        LocalDateTime fechaPago,
        LocalDateTime creadoEn
) {

    public static PagoVentaResponse desdeEntidad(PagoVenta pagoVenta) {
        return new PagoVentaResponse(
                pagoVenta.getId(),
                pagoVenta.getVenta().getId(),
                pagoVenta.getMonto(),
                pagoVenta.getMetodoPago(),
                pagoVenta.getReferencia(),
                pagoVenta.getFechaPago(),
                pagoVenta.getCreadoEn()
        );
    }
}
