package com.agroempresa.erp.finanzas.cartera.dto;

import com.agroempresa.erp.comercial.compra.Compra;
import com.agroempresa.erp.finanzas.EstadoPago;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public record CuentaPorPagarResponse(
        Long compraId,
        Long proveedorId,
        String proveedorNombre,
        LocalDateTime fechaCompra,
        LocalDate fechaVencimiento,
        BigDecimal total,
        BigDecimal totalPagado,
        BigDecimal saldoPendiente,
        EstadoPago estadoPago,
        boolean vencida,
        long diasVencida
) {

    public static CuentaPorPagarResponse desdeEntidad(Compra compra) {
        LocalDate hoy = LocalDate.now();
        boolean vencida = compra.getFechaVencimiento().isBefore(hoy);

        return new CuentaPorPagarResponse(
                compra.getId(),
                compra.getProveedor().getId(),
                compra.getProveedor().getNombre(),
                compra.getFechaCompra(),
                compra.getFechaVencimiento(),
                compra.getTotal(),
                compra.getTotalPagado(),
                compra.getSaldoPendiente(),
                compra.getEstadoPago(),
                vencida,
                vencida ? ChronoUnit.DAYS.between(compra.getFechaVencimiento(), hoy) : 0
        );
    }
}
