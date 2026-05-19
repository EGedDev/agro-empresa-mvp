package com.agroempresa.erp.comercial.venta.devolucion.dto;

import com.agroempresa.erp.comercial.venta.devolucion.DevolucionVentaDetalle;

import java.math.BigDecimal;

public record DevolucionVentaDetalleResponse(
        Long id,
        Long ventaDetalleId,
        Long productoId,
        String productoNombre,
        Integer cantidad,
        BigDecimal precioUnitario,
        BigDecimal subtotal
) {

    public static DevolucionVentaDetalleResponse desdeEntidad(DevolucionVentaDetalle detalle) {
        return new DevolucionVentaDetalleResponse(
                detalle.getId(),
                detalle.getVentaDetalle().getId(),
                detalle.getProducto().getId(),
                detalle.getProducto().getNombre(),
                detalle.getCantidad(),
                detalle.getPrecioUnitario(),
                detalle.getSubtotal()
        );
    }
}
