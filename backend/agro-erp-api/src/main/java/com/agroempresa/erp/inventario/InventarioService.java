package com.agroempresa.erp.inventario;

import com.agroempresa.erp.catalogo.producto.Producto;
import com.agroempresa.erp.catalogo.producto.ProductoRepository;
import com.agroempresa.erp.auditoria.AuditoriaService;
import com.agroempresa.erp.common.error.BusinessException;
import com.agroempresa.erp.common.error.RecursoNoEncontradoException;
import com.agroempresa.erp.common.pagination.PaginaResponse;
import com.agroempresa.erp.common.pagination.Paginacion;
import com.agroempresa.erp.inventario.dto.MovimientoInventarioResponse;
import com.agroempresa.erp.inventario.dto.RegistrarMovimientoInventarioRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Service
public class InventarioService {

    private static final int ESCALA_COSTO = 4;
    private static final int ESCALA_VALOR = 2;

    private static final Map<String, String> CAMPOS_ORDENABLES = Map.of(
            "id", "id",
            "creadoEn", "creadoEn",
            "tipo", "tipo",
            "cantidad", "cantidad",
            "stockAnterior", "stockAnterior",
            "stockNuevo", "stockNuevo",
            "valorMovimiento", "valorMovimiento",
            "valorInventarioNuevo", "valorInventarioNuevo"
    );

    private static final Sort ORDEN_DEFAULT = Sort.by(Sort.Direction.DESC, "creadoEn");

    private final MovimientoInventarioRepository movimientoInventarioRepository;
    private final ProductoRepository productoRepository;
    private final AuditoriaService auditoriaService;

    public InventarioService(
            MovimientoInventarioRepository movimientoInventarioRepository,
            ProductoRepository productoRepository,
            AuditoriaService auditoriaService
    ) {
        this.movimientoInventarioRepository = movimientoInventarioRepository;
        this.productoRepository = productoRepository;
        this.auditoriaService = auditoriaService;
    }

    @Transactional(readOnly = true)
    public PaginaResponse<MovimientoInventarioResponse> listarMovimientos(
            Long productoId,
            TipoMovimientoInventario tipo,
            String referenciaTipo,
            LocalDate desde,
            LocalDate hasta,
            Integer pagina,
            Integer tamanio,
            String orden
    ) {
        validarProductoSiFueInformado(productoId);
        validarRangoFechas(desde, hasta);

        return PaginaResponse.desde(
                movimientoInventarioRepository.findAll(
                        filtroMovimientos(productoId, tipo, Paginacion.normalizarTexto(referenciaTipo), inicioDia(desde), inicioDiaPosterior(hasta)),
                        Paginacion.crear(pagina, tamanio, orden, CAMPOS_ORDENABLES, ORDEN_DEFAULT)
                ),
                MovimientoInventarioResponse::desde
        );
    }

    private Specification<MovimientoInventario> filtroMovimientos(
            Long productoId,
            TipoMovimientoInventario tipo,
            String referenciaTipo,
            LocalDateTime desde,
            LocalDateTime hastaExclusivo
    ) {
        return (root, query, criteriaBuilder) -> {
            var filtros = criteriaBuilder.conjunction();

            if (productoId != null) {
                filtros = criteriaBuilder.and(filtros, criteriaBuilder.equal(root.get("producto").get("id"), productoId));
            }

            if (tipo != null) {
                filtros = criteriaBuilder.and(filtros, criteriaBuilder.equal(root.get("tipo"), tipo));
            }

            if (referenciaTipo != null) {
                filtros = criteriaBuilder.and(
                        filtros,
                        criteriaBuilder.equal(
                                criteriaBuilder.lower(criteriaBuilder.coalesce(root.<String>get("referenciaTipo"), "")),
                                referenciaTipo
                        )
                );
            }

            if (desde != null) {
                filtros = criteriaBuilder.and(filtros, criteriaBuilder.greaterThanOrEqualTo(root.<LocalDateTime>get("creadoEn"), desde));
            }

            if (hastaExclusivo != null) {
                filtros = criteriaBuilder.and(filtros, criteriaBuilder.lessThan(root.<LocalDateTime>get("creadoEn"), hastaExclusivo));
            }

            return filtros;
        };
    }

    @Transactional(readOnly = true)
    public PaginaResponse<MovimientoInventarioResponse> listarMovimientosPorProducto(
            Long productoId,
            Integer pagina,
            Integer tamanio,
            String orden
    ) {
        if (productoId == null) {
            throw new BusinessException("El producto es obligatorio");
        }

        return listarMovimientos(productoId, null, null, null, null, pagina, tamanio, orden);
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
    public void registrarEntradaPorDevolucionVenta(
            Producto producto,
            Integer cantidad,
            Integer stockAnterior,
            Integer stockNuevo,
            Long devolucionVentaId
    ) {
        registrarMovimiento(
                producto,
                TipoMovimientoInventario.ENTRADA_POR_DEVOLUCION_VENTA,
                cantidad,
                stockAnterior,
                stockNuevo,
                "Entrada de inventario por devolucion de venta",
                "DEVOLUCION_VENTA",
                devolucionVentaId
        );
    }

    @Transactional
    public void registrarEntradaPorCompra(
            Producto producto,
            Integer cantidad,
            Integer stockAnterior,
            Integer stockNuevo,
            Long compraId
    ) {
        registrarMovimiento(
                producto,
                TipoMovimientoInventario.ENTRADA_POR_COMPRA,
                cantidad,
                stockAnterior,
                stockNuevo,
                "Entrada de inventario por compra registrada",
                "COMPRA",
                compraId
        );
    }

    @Transactional
    public void registrarSalidaPorCancelacionCompra(
            Producto producto,
            Integer cantidad,
            Integer stockAnterior,
            Integer stockNuevo,
            Long compraId
    ) {
        registrarMovimiento(
                producto,
                TipoMovimientoInventario.SALIDA_POR_CANCELACION_COMPRA,
                cantidad,
                stockAnterior,
                stockNuevo,
                "Salida de inventario por cancelación de compra",
                "COMPRA",
                compraId
        );
    }

    @Transactional
    public void registrarSalidaPorDevolucionCompra(
            Producto producto,
            Integer cantidad,
            Integer stockAnterior,
            Integer stockNuevo,
            Long devolucionCompraId
    ) {
        registrarMovimiento(
                producto,
                TipoMovimientoInventario.SALIDA_POR_DEVOLUCION_COMPRA,
                cantidad,
                stockAnterior,
                stockNuevo,
                "Salida de inventario por devolucion de compra",
                "DEVOLUCION_COMPRA",
                devolucionCompraId
        );
    }

    @Transactional
    public void registrarSalidaPorVenta(
            Producto producto,
            Integer cantidad,
            Integer stockAnterior,
            Integer stockNuevo,
            Long ventaId,
            BigDecimal costoUnitario,
            BigDecimal valorInventarioAnterior,
            BigDecimal valorInventarioNuevo
    ) {
        registrarMovimientoValorizado(
                producto,
                TipoMovimientoInventario.SALIDA_POR_VENTA,
                cantidad,
                stockAnterior,
                stockNuevo,
                costoUnitario,
                valorInventarioAnterior,
                valorInventarioNuevo,
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
            Long ventaId,
            BigDecimal costoUnitario,
            BigDecimal valorInventarioAnterior,
            BigDecimal valorInventarioNuevo
    ) {
        registrarMovimientoValorizado(
                producto,
                TipoMovimientoInventario.ENTRADA_POR_CANCELACION,
                cantidad,
                stockAnterior,
                stockNuevo,
                costoUnitario,
                valorInventarioAnterior,
                valorInventarioNuevo,
                "Entrada de inventario por cancelacion de venta",
                "VENTA",
                ventaId
        );
    }

    @Transactional
    public void registrarEntradaPorDevolucionVenta(
            Producto producto,
            Integer cantidad,
            Integer stockAnterior,
            Integer stockNuevo,
            Long devolucionVentaId,
            BigDecimal costoUnitario,
            BigDecimal valorInventarioAnterior,
            BigDecimal valorInventarioNuevo
    ) {
        registrarMovimientoValorizado(
                producto,
                TipoMovimientoInventario.ENTRADA_POR_DEVOLUCION_VENTA,
                cantidad,
                stockAnterior,
                stockNuevo,
                costoUnitario,
                valorInventarioAnterior,
                valorInventarioNuevo,
                "Entrada de inventario por devolucion de venta",
                "DEVOLUCION_VENTA",
                devolucionVentaId
        );
    }

    @Transactional
    public void registrarEntradaPorCompra(
            Producto producto,
            Integer cantidad,
            Integer stockAnterior,
            Integer stockNuevo,
            Long compraId,
            BigDecimal costoUnitario,
            BigDecimal valorInventarioAnterior,
            BigDecimal valorInventarioNuevo
    ) {
        registrarMovimientoValorizado(
                producto,
                TipoMovimientoInventario.ENTRADA_POR_COMPRA,
                cantidad,
                stockAnterior,
                stockNuevo,
                costoUnitario,
                valorInventarioAnterior,
                valorInventarioNuevo,
                "Entrada de inventario por compra registrada",
                "COMPRA",
                compraId
        );
    }

    @Transactional
    public void registrarSalidaPorCancelacionCompra(
            Producto producto,
            Integer cantidad,
            Integer stockAnterior,
            Integer stockNuevo,
            Long compraId,
            BigDecimal costoUnitario,
            BigDecimal valorInventarioAnterior,
            BigDecimal valorInventarioNuevo
    ) {
        registrarMovimientoValorizado(
                producto,
                TipoMovimientoInventario.SALIDA_POR_CANCELACION_COMPRA,
                cantidad,
                stockAnterior,
                stockNuevo,
                costoUnitario,
                valorInventarioAnterior,
                valorInventarioNuevo,
                "Salida de inventario por cancelacion de compra",
                "COMPRA",
                compraId
        );
    }

    @Transactional
    public void registrarSalidaPorDevolucionCompra(
            Producto producto,
            Integer cantidad,
            Integer stockAnterior,
            Integer stockNuevo,
            Long devolucionCompraId,
            BigDecimal costoUnitario,
            BigDecimal valorInventarioAnterior,
            BigDecimal valorInventarioNuevo
    ) {
        registrarMovimientoValorizado(
                producto,
                TipoMovimientoInventario.SALIDA_POR_DEVOLUCION_COMPRA,
                cantidad,
                stockAnterior,
                stockNuevo,
                costoUnitario,
                valorInventarioAnterior,
                valorInventarioNuevo,
                "Salida de inventario por devolucion de compra",
                "DEVOLUCION_COMPRA",
                devolucionCompraId
        );
    }

    @Transactional
    public MovimientoInventarioResponse registrarMovimientoManual(RegistrarMovimientoInventarioRequest request) {
        Producto producto = productoRepository.findByIdParaActualizar(request.productoId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró el producto con id: " + request.productoId()
                ));

        if (!producto.getActivo()) {
            throw new BusinessException("No se puede modificar inventario de un producto inactivo");
        }

        TipoMovimientoInventario tipo = request.tipo();
        Integer cantidad = request.cantidad();
        Integer stockAnterior = producto.getStockActual();
        BigDecimal valorInventarioAnterior = producto.getValorInventario();
        BigDecimal costoUnitario = request.costoUnitario() == null
                ? producto.getCostoPromedio()
                : request.costoUnitario();

        switch (tipo) {
            case ENTRADA_MANUAL, AJUSTE_POSITIVO -> producto.aumentarStockConCosto(cantidad, costoUnitario);
            case AJUSTE_NEGATIVO -> {
                costoUnitario = producto.getCostoPromedio();
                producto.descontarStockConCosto(cantidad, costoUnitario);
            }
            default -> throw new BusinessException("Tipo de movimiento no permitido para registro manual");
        }

        MovimientoInventario movimiento = registrarMovimientoValorizado(
                producto,
                tipo,
                cantidad,
                stockAnterior,
                producto.getStockActual(),
                costoUnitario,
                valorInventarioAnterior,
                producto.getValorInventario(),
                request.motivo().trim(),
                "INVENTARIO_MANUAL",
                null
        );

        auditoriaService.registrar(
                "INVENTARIO_MOVIMIENTO_MANUAL",
                "PRODUCTO",
                producto.getId(),
                "Tipo: " + tipo + ", cantidad: " + cantidad
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

    private MovimientoInventario registrarMovimientoValorizado(
            Producto producto,
            TipoMovimientoInventario tipo,
            Integer cantidad,
            Integer stockAnterior,
            Integer stockNuevo,
            BigDecimal costoUnitario,
            BigDecimal valorInventarioAnterior,
            BigDecimal valorInventarioNuevo,
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
                costoUnitario,
                valorMovimiento(cantidad, costoUnitario),
                valorInventarioAnterior,
                valorInventarioNuevo,
                motivo,
                referenciaTipo,
                referenciaId
        );

        return movimientoInventarioRepository.save(movimiento);
    }

    private BigDecimal valorMovimiento(Integer cantidad, BigDecimal costoUnitario) {
        BigDecimal costo = costoUnitario == null ? BigDecimal.ZERO : costoUnitario;
        return costo.setScale(ESCALA_COSTO, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(cantidad))
                .setScale(ESCALA_VALOR, RoundingMode.HALF_UP);
    }

    private void validarProductoSiFueInformado(Long productoId) {
        if (productoId != null && !productoRepository.existsById(productoId)) {
            throw new RecursoNoEncontradoException("No se encontró el producto con id: " + productoId);
        }
    }

    private void validarRangoFechas(LocalDate desde, LocalDate hasta) {
        if (desde != null && hasta != null && hasta.isBefore(desde)) {
            throw new BusinessException("La fecha final no puede ser anterior a la fecha inicial");
        }
    }

    private LocalDateTime inicioDia(LocalDate fecha) {
        return fecha == null ? null : fecha.atStartOfDay();
    }

    private LocalDateTime inicioDiaPosterior(LocalDate fecha) {
        return fecha == null ? null : fecha.plusDays(1).atStartOfDay();
    }
}
