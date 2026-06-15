package com.agroempresa.erp.catalogo.producto.dto;

import com.agroempresa.erp.catalogo.producto.Producto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductoResponse(
        Long id,
        String nombre,
        String descripcion,
        BigDecimal precioVenta,
        BigDecimal costoPromedio,
        BigDecimal valorInventario,
        Integer stockActual,
        Integer stockMinimo,
        Boolean stockBajo,
        String imagenUrl,
        String imagenAlt,
        String resumenComercial,
        Boolean visibleWeb,
        Boolean destacado,
        Integer ordenWeb,
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
                producto.getCostoPromedio(),
                producto.getValorInventario(),
                producto.getStockActual(),
                producto.getStockMinimo(),
                stockBajo,
                producto.getImagenUrl(),
                producto.getImagenAlt(),
                producto.getResumenComercial(),
                producto.getVisibleWeb(),
                producto.getDestacado(),
                producto.getOrdenWeb(),
                producto.getActivo(),
                producto.getCategoria().getId(),
                producto.getCategoria().getNombre(),
                producto.getCreadoEn(),
                producto.getActualizadoEn()
        );
    }
}
