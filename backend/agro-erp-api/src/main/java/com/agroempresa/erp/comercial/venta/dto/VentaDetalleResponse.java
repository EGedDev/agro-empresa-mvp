package com.agroempresa.erp.comercial.venta.dto;

import com.agroempresa.erp.comercial.venta.VentaDetalle;

import java.math.BigDecimal;

public record VentaDetalleResponse(
        Long id,
        Long productoId,
        String productoNombre,
        Integer cantidad,
        BigDecimal precioUnitario,
        BigDecimal subtotal
) {

    public static VentaDetalleResponse desdeEntidad(VentaDetalle detalle) {
        return new VentaDetalleResponse(
                detalle.getId(),
                detalle.getProducto().getId(),
                detalle.getProducto().getNombre(),
                detalle.getCantidad(),
                detalle.getPrecioUnitario(),
                detalle.getSubtotal()
        );
    }
}