package com.agroempresa.erp.comercial.compra.dto;

import com.agroempresa.erp.comercial.compra.Compra;
import com.agroempresa.erp.comercial.compra.EstadoCompra;
import com.agroempresa.erp.finanzas.EstadoPago;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record CompraResponse(
        Long id,
        Long proveedorId,
        String proveedorNombre,
        LocalDateTime fechaCompra,
        LocalDate fechaVencimiento,
        EstadoCompra estado,
        BigDecimal total,
        BigDecimal totalPagado,
        BigDecimal saldoPendiente,
        EstadoPago estadoPago,
        List<CompraDetalleResponse> detalles,
        LocalDateTime creadoEn,
        LocalDateTime actualizadoEn
) {

    public static CompraResponse desdeEntidad(Compra compra) {
        return new CompraResponse(
                compra.getId(),
                compra.getProveedor().getId(),
                compra.getProveedor().getNombre(),
                compra.getFechaCompra(),
                compra.getFechaVencimiento(),
                compra.getEstado(),
                compra.getTotal(),
                compra.getTotalPagado(),
                compra.getSaldoPendiente(),
                compra.getEstadoPago(),
                compra.getDetalles()
                        .stream()
                        .map(CompraDetalleResponse::desdeEntidad)
                        .toList(),
                compra.getCreadoEn(),
                compra.getActualizadoEn()
        );
    }
}
