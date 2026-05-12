package com.agroempresa.erp.comercial.venta.dto;

public record VentaDetalleRequest(
        Long productoId,
        Integer cantidad
) {
}