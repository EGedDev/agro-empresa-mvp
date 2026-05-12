package com.agroempresa.erp.catalogo.producto.dto;

import com.agroempresa.erp.catalogo.producto.Producto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductoResponse(
        Long id,
        String nombre,
        String descripcion,
        BigDecimal precioVenta,
        Integer stockActual,
        Integer stockMinimo,
        Boolean stockBajo,
        Boolean activo,
        Long categoriaId,
        String categoriaNombre,
        LocalDateTime creadoEn,
        LocalDateTime actualizadoEn
) {

    public static ProductoResponse desdeEntidad(Producto producto) {
        boolean stockBajo = producto.getStockActual() <= producto.getStockMinimo();

        return new ProductoResponse(
                producto.getId(),
                producto.getNombre(),
                producto.getDescripcion(),
                producto.getPrecioVenta(),
                producto.getStockActual(),
                producto.getStockMinimo(),
                stockBajo,
                producto.getActivo(),
                producto.getCategoria().getId(),
                producto.getCategoria().getNombre(),
                producto.getCreadoEn(),
                producto.getActualizadoEn()
        );
    }
}