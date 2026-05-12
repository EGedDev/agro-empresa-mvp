package com.agroempresa.erp.inventario;

import com.agroempresa.erp.catalogo.producto.Producto;
import com.agroempresa.erp.catalogo.producto.ProductoRepository;
import com.agroempresa.erp.common.error.BusinessException;
import com.agroempresa.erp.common.error.RecursoNoEncontradoException;
import com.agroempresa.erp.inventario.dto.MovimientoInventarioResponse;
import com.agroempresa.erp.inventario.dto.RegistrarMovimientoInventarioRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InventarioService {

    private final MovimientoInventarioRepository movimientoInventarioRepository;
    private final ProductoRepository productoRepository;

    public InventarioService(
            MovimientoInventarioRepository movimientoInventarioRepository,
            ProductoRepository productoRepository
    ) {
        this.movimientoInventarioRepository = movimientoInventarioRepository;
        this.productoRepository = productoRepository;
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
        registrarMovimiento(
                producto,
                TipoMovimientoInventario.SALIDA_POR_VENTA,
                cantidad,
                stockAnterior,
                stockNuevo,
                "Salida de inventario por venta registrada",
                "VENTA",
                ventaId
        );
    }

    @Transactional
    public void registrarEntradaPorCancelacionVenta(
            Producto producto,
            Integer cantidad,
            Integer stockAnterior,
            Integer stockNuevo,
            Long ventaId
    ) {
        registrarMovimiento(
                producto,
                TipoMovimientoInventario.ENTRADA_POR_CANCELACION,
                cantidad,
                stockAnterior,
                stockNuevo,
                "Entrada de inventario por cancelación de venta",
                "VENTA",
                ventaId
        );
    }

    @Transactional
    public MovimientoInventarioResponse registrarMovimientoManual(RegistrarMovimientoInventarioRequest request) {
        Producto producto = productoRepository.findById(request.productoId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró el producto con id: " + request.productoId()
                ));

        if (!producto.getActivo()) {
            throw new BusinessException("No se puede modificar inventario de un producto inactivo");
        }

        TipoMovimientoInventario tipo = request.tipo();
        Integer cantidad = request.cantidad();
        Integer stockAnterior = producto.getStockActual();

        switch (tipo) {
            case ENTRADA_MANUAL, AJUSTE_POSITIVO -> producto.aumentarStock(cantidad);
            case AJUSTE_NEGATIVO -> producto.descontarStock(cantidad);
            default -> throw new BusinessException("Tipo de movimiento no permitido para registro manual");
        }

        MovimientoInventario movimiento = registrarMovimiento(
                producto,
                tipo,
                cantidad,
                stockAnterior,
                producto.getStockActual(),
                request.motivo().trim(),
                "INVENTARIO_MANUAL",
                null
        );

        return MovimientoInventarioResponse.desde(movimiento);
    }

    private MovimientoInventario registrarMovimiento(
            Producto producto,
            TipoMovimientoInventario tipo,
            Integer cantidad,
            Integer stockAnterior,
            Integer stockNuevo,
            String motivo,
            String referenciaTipo,
            Long referenciaId
    ) {
        MovimientoInventario movimiento = new MovimientoInventario(
                producto,
                tipo,
                cantidad,
                stockAnterior,
                stockNuevo,
                motivo,
                referenciaTipo,
                referenciaId
        );

        return movimientoInventarioRepository.save(movimiento);
    }
}
