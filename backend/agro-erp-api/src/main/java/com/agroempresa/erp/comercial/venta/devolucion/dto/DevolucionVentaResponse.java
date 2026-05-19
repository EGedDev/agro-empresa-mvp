package com.agroempresa.erp.comercial.venta.devolucion.dto;

import com.agroempresa.erp.comercial.venta.devolucion.DevolucionVenta;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record DevolucionVentaResponse(
        Long id,
        Long ventaId,
        LocalDateTime fechaDevolucion,
        String motivo,
        BigDecimal total,
        List<DevolucionVentaDetalleResponse> detalles,
        LocalDateTime creadoEn
) {

    public static DevolucionVentaResponse desdeEntidad(DevolucionVenta devolucion) {
        return new DevolucionVentaResponse(
                devolucion.getId(),
                devolucion.getVenta().getId(),
                devolucion.getFechaDevolucion(),
                devolucion.getMotivo(),
                devolucion.getTotal(),
                devolucion.getDetalles()
                        .stream()
                        .map(DevolucionVentaDetalleResponse::desdeEntidad)
                        .toList(),
                devolucion.getCreadoEn()
        );
    }
}
