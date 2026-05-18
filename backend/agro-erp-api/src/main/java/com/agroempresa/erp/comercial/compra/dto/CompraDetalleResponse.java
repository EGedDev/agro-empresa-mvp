package com.agroempresa.erp.comercial.compra.dto;

import com.agroempresa.erp.comercial.compra.CompraDetalle;

import java.math.BigDecimal;

public record CompraDetalleResponse(
        Long id,
        Long productoId,
        String productoNombre,
        Integer cantidad,
        BigDecimal costoUnitario,
        BigDecimal subtotal
) {

    public static CompraDetalleResponse desdeEntidad(CompraDetalle detalle) {
        return new CompraDetalleResponse(
                detalle.getId(),
                detalle.getProducto().getId(),
                detalle.getProducto().getNombre(),
                detalle.getCantidad(),
                detalle.getCostoUnitario(),
                detalle.getSubtotal()
        );
    }
}
