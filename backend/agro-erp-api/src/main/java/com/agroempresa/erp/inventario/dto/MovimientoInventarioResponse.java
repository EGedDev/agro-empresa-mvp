package com.agroempresa.erp.inventario.dto;

import com.agroempresa.erp.inventario.MovimientoInventario;
import com.agroempresa.erp.inventario.TipoMovimientoInventario;

import java.time.LocalDateTime;

public record MovimientoInventarioResponse(
        Long id,
        Long productoId,
        String productoNombre,
        TipoMovimientoInventario tipo,
        Integer cantidad,
        Integer stockAnterior,
        Integer stockNuevo,
        String motivo,
        String referenciaTipo,
        Long referenciaId,
        LocalDateTime creadoEn
) {

    public static MovimientoInventarioResponse desde(MovimientoInventario movimiento) {
        return new MovimientoInventarioResponse(
                movimiento.getId(),
                movimiento.getProducto().getId(),
                movimiento.getProducto().getNombre(),
                movimiento.getTipo(),
                movimiento.getCantidad(),
                movimiento.getStockAnterior(),
                movimiento.getStockNuevo(),
                movimiento.getMotivo(),
                movimiento.getReferenciaTipo(),
                movimiento.getReferenciaId(),
                movimiento.getCreadoEn()
        );
    }
}