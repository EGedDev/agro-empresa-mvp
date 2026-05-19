package com.agroempresa.erp.comercial.compra.devolucion.dto;

import com.agroempresa.erp.comercial.compra.devolucion.DevolucionCompra;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record DevolucionCompraResponse(
        Long id,
        String numero,
        Long compraId,
        LocalDateTime fechaDevolucion,
        String motivo,
        BigDecimal total,
        List<DevolucionCompraDetalleResponse> detalles,
        LocalDateTime creadoEn
) {

    public static DevolucionCompraResponse desdeEntidad(DevolucionCompra devolucion) {
        return new DevolucionCompraResponse(
                devolucion.getId(),
                devolucion.getNumero(),
                devolucion.getCompra().getId(),
                devolucion.getFechaDevolucion(),
                devolucion.getMotivo(),
                devolucion.getTotal(),
                devolucion.getDetalles()
                        .stream()
                        .map(DevolucionCompraDetalleResponse::desdeEntidad)
                        .toList(),
                devolucion.getCreadoEn()
        );
    }
}
