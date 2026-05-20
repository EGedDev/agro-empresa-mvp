package com.agroempresa.erp.inventario.dto;

import com.agroempresa.erp.inventario.MovimientoInventario;
import com.agroempresa.erp.inventario.TipoMovimientoInventario;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MovimientoInventarioResponse(
        Long id,
        Long productoId,
        String productoNombre,
        TipoMovimientoInventario tipo,
        Integer cantidad,
        Integer stockAnterior,
        Integer stockNuevo,
        BigDecimal costoUnitario,
        BigDecimal valorMovimiento,
        BigDecimal valorInventarioAnterior,
        BigDecimal valorInventarioNuevo,
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
                movimiento.getCostoUnitario(),
                movimiento.getValorMovimiento(),
                movimiento.getValorInventarioAnterior(),
                movimiento.getValorInventarioNuevo(),
                movimiento.getMotivo(),
                movimiento.getReferenciaTipo(),
                movimiento.getReferenciaId(),
                movimiento.getCreadoEn()
        );
    }
}
