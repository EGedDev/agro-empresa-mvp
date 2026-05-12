package com.agroempresa.erp.inventario;

import com.agroempresa.erp.catalogo.producto.Producto;
import com.agroempresa.erp.inventario.dto.MovimientoInventarioResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InventarioService {

    private final MovimientoInventarioRepository movimientoInventarioRepository;

    public InventarioService(MovimientoInventarioRepository movimientoInventarioRepository) {
        this.movimientoInventarioRepository = movimientoInventarioRepository;
    }

    @Transactional(readOnly = true)
    public List<MovimientoInventarioResponse> listarUltimosMovimientos() {
        return movimientoInventarioRepository.findTop30ByOrderByCreadoEnDesc()
                .stream()
                .map(MovimientoInventarioResponse::desde)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MovimientoInventarioResponse> listarMovimientosPorProducto(Long productoId) {
        return movimientoInventarioRepository.findByProductoIdOrderByCreadoEnDesc(productoId)
                .stream()
                .map(MovimientoInventarioResponse::desde)
                .toList();
    }

    @Transactional
    public void registrarSalidaPorVenta(
            Producto producto,
            Integer cantidad,
            Integer stockAnterior,
            Integer stockNuevo,
            Long ventaId
    ) {
        MovimientoInventario movimiento = new MovimientoInventario(
                producto,
                TipoMovimientoInventario.SALIDA_POR_VENTA,
                cantidad,
                stockAnterior,
                stockNuevo,
                "Salida de inventario por venta registrada",
                "VENTA",
                ventaId
        );

        movimientoInventarioRepository.save(movimiento);
    }
}