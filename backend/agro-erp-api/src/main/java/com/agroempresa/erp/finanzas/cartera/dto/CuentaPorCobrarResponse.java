package com.agroempresa.erp.finanzas.cartera.dto;

import com.agroempresa.erp.comercial.venta.Venta;
import com.agroempresa.erp.finanzas.EstadoPago;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public record CuentaPorCobrarResponse(
        Long ventaId,
        Long clienteId,
        String clienteNombre,
        LocalDateTime fechaVenta,
        LocalDate fechaVencimiento,
        BigDecimal total,
        BigDecimal totalPagado,
        BigDecimal saldoPendiente,
        EstadoPago estadoPago,
        boolean vencida,
        long diasVencida
) {

    public static CuentaPorCobrarResponse desdeEntidad(Venta venta) {
        LocalDate hoy = LocalDate.now();
        boolean vencida = venta.getFechaVencimiento().isBefore(hoy);

        return new CuentaPorCobrarResponse(
                venta.getId(),
                venta.getCliente().getId(),
                venta.getCliente().getNombre(),
                venta.getFechaVenta(),
                venta.getFechaVencimiento(),
                venta.getTotal(),
                venta.getTotalPagado(),
                venta.getSaldoPendiente(),
                venta.getEstadoPago(),
                vencida,
                vencida ? ChronoUnit.DAYS.between(venta.getFechaVencimiento(), hoy) : 0
        );
    }
}
