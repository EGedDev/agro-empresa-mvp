package com.agroempresa.erp.comercial.compra.devolucion.dto;

import com.agroempresa.erp.comercial.compra.devolucion.DevolucionCompraDetalle;

import java.math.BigDecimal;

public record DevolucionCompraDetalleResponse(
        Long id,
        Long compraDetalleId,
        Long productoId,
        String productoNombre,
        Integer cantidad,
        BigDecimal costoUnitario,
        BigDecimal subtotal
) {

    public static DevolucionCompraDetalleResponse desdeEntidad(DevolucionCompraDetalle detalle) {
        return new DevolucionCompraDetalleResponse(
                detalle.getId(),
                detalle.getCompraDetalle().getId(),
                detalle.getProducto().getId(),
                detalle.getProducto().getNombre(),
                detalle.getCantidad(),
                detalle.getCostoUnitario(),
                detalle.getSubtotal()
        );
    }
}
