package com.agroempresa.erp.comercial.venta.dto;

import com.agroempresa.erp.comercial.venta.EstadoVenta;
import com.agroempresa.erp.comercial.venta.Venta;
import com.agroempresa.erp.finanzas.EstadoPago;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record VentaResponse(
        Long id,
        Long clienteId,
        String clienteNombre,
        LocalDateTime fechaVenta,
        EstadoVenta estado,
        BigDecimal total,
        BigDecimal totalPagado,
        BigDecimal saldoPendiente,
        EstadoPago estadoPago,
        List<VentaDetalleResponse> detalles,
        LocalDateTime creadoEn,
        LocalDateTime actualizadoEn
) {

    public static VentaResponse desdeEntidad(Venta venta) {
        return new VentaResponse(
                venta.getId(),
                venta.getCliente().getId(),
                venta.getCliente().getNombre(),
                venta.getFechaVenta(),
                venta.getEstado(),
                venta.getTotal(),
                venta.getTotalPagado(),
                venta.getSaldoPendiente(),
                venta.getEstadoPago(),
                venta.getDetalles()
                        .stream()
                        .map(VentaDetalleResponse::desdeEntidad)
                        .toList(),
                venta.getCreadoEn(),
                venta.getActualizadoEn()
        );
    }
}
