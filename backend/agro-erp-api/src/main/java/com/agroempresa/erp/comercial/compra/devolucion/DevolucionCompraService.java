package com.agroempresa.erp.comercial.compra.devolucion;

import com.agroempresa.erp.auditoria.AuditoriaService;
import com.agroempresa.erp.catalogo.producto.Producto;
import com.agroempresa.erp.catalogo.producto.ProductoRepository;
import com.agroempresa.erp.comercial.compra.Compra;
import com.agroempresa.erp.comercial.compra.CompraDetalle;
import com.agroempresa.erp.comercial.compra.CompraRepository;
import com.agroempresa.erp.comercial.compra.EstadoCompra;
import com.agroempresa.erp.comercial.compra.devolucion.dto.DevolucionCompraDetalleRequest;
import com.agroempresa.erp.comercial.compra.devolucion.dto.DevolucionCompraResponse;
import com.agroempresa.erp.comercial.compra.devolucion.dto.RegistrarDevolucionCompraRequest;
import com.agroempresa.erp.common.error.BusinessException;
import com.agroempresa.erp.common.error.RecursoNoEncontradoException;
import com.agroempresa.erp.common.numeracion.NumeroDocumento;
import com.agroempresa.erp.common.numeracion.NumeracionService;
import com.agroempresa.erp.common.numeracion.TipoDocumento;
import com.agroempresa.erp.common.pagination.PaginaResponse;
import com.agroempresa.erp.common.pagination.Paginacion;
import com.agroempresa.erp.inventario.InventarioService;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class DevolucionCompraService {

    private static final Map<String, String> CAMPOS_ORDENABLES = Map.of(
            "id", "id",
            "numero", "numero",
            "fechaDevolucion", "fechaDevolucion",
            "total", "total",
            "creadoEn", "creadoEn"
    );

    private static final Sort ORDEN_DEFAULT = Sort.by(Sort.Direction.DESC, "fechaDevolucion");

    private final DevolucionCompraRepository devolucionCompraRepository;
    private final CompraRepository compraRepository;
    private final ProductoRepository productoRepository;
    private final InventarioService inventarioService;
    private final AuditoriaService auditoriaService;
    private final NumeracionService numeracionService;

    public DevolucionCompraService(
            DevolucionCompraRepository devolucionCompraRepository,
            CompraRepository compraRepository,
            ProductoRepository productoRepository,
            InventarioService inventarioService,
            AuditoriaService auditoriaService,
            NumeracionService numeracionService
    ) {
        this.devolucionCompraRepository = devolucionCompraRepository;
        this.compraRepository = compraRepository;
        this.productoRepository = productoRepository;
        this.inventarioService = inventarioService;
        this.auditoriaService = auditoriaService;
        this.numeracionService = numeracionService;
    }

    @Transactional(readOnly = true)
    public PaginaResponse<DevolucionCompraResponse> listarPorCompra(
            Long compraId,
            String numero,
            Integer pagina,
            Integer tamanio,
            String orden
    ) {
        if (compraId == null) {
            throw new BusinessException("La compra es obligatoria");
        }

        if (!compraRepository.existsById(compraId)) {
            throw new RecursoNoEncontradoException("No se encontro la compra con id: " + compraId);
        }

        return PaginaResponse.desde(
                devolucionCompraRepository.buscarPorCompra(
                        compraId,
                        NumeroDocumento.normalizarFiltro(numero),
                        Paginacion.crear(pagina, tamanio, orden, CAMPOS_ORDENABLES, ORDEN_DEFAULT)
                ),
                DevolucionCompraResponse::desdeEntidad
        );
    }

    @Transactional
    public DevolucionCompraResponse registrar(Long compraId, RegistrarDevolucionCompraRequest request) {
        if (compraId == null) {
            throw new BusinessException("La compra es obligatoria");
        }

        validarRequest(request);

        Compra compra = compraRepository.findByIdParaActualizar(compraId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontro la compra con id: " + compraId
                ));

        if (compra.getEstado() == EstadoCompra.CANCELADA) {
            throw new BusinessException("No se puede registrar devoluciones para una compra cancelada");
        }

        Map<Long, Integer> cantidadesPorDetalle = consolidarDetalles(request.detalles());
        List<DetalleDevolucionCompraValidado> detallesValidados = validarDetalles(compra, cantidadesPorDetalle);
        Map<Long, Producto> productos = buscarProductosParaActualizar(detallesValidados);

        DevolucionCompra devolucion = new DevolucionCompra(compra, normalizarMotivo(request.motivo()));
        devolucion.asignarNumero(numeracionService.generar(TipoDocumento.DEVOLUCION_COMPRA));
        List<MovimientoInventarioPendiente> movimientosPendientes = new ArrayList<>();

        for (DetalleDevolucionCompraValidado detalleValidado : detallesValidados) {
            Producto producto = productos.get(detalleValidado.producto().getId());
            CompraDetalle compraDetalle = detalleValidado.compraDetalle();
            Integer stockAnterior = producto.getStockActual();
            BigDecimal costoUnitario = compraDetalle.getCostoUnitario();
            BigDecimal valorInventarioAnterior = producto.getValorInventario();

            try {
                producto.descontarStockConCosto(detalleValidado.cantidad(), costoUnitario);
            } catch (IllegalArgumentException ex) {
                throw new BusinessException(
                        "Stock insuficiente para devolver la compra del producto: " + producto.getNombre()
                );
            }

            devolucion.agregarDetalle(compraDetalle, detalleValidado.cantidad());

            movimientosPendientes.add(new MovimientoInventarioPendiente(
                    producto,
                    detalleValidado.cantidad(),
                    stockAnterior,
                    producto.getStockActual(),
                    costoUnitario,
                    valorInventarioAnterior,
                    producto.getValorInventario()
            ));
        }

        try {
            compra.registrarDevolucion(devolucion.getTotal());
        } catch (IllegalArgumentException | IllegalStateException ex) {
            throw new BusinessException(ex.getMessage());
        }

        DevolucionCompra devolucionGuardada = devolucionCompraRepository.save(devolucion);

        for (MovimientoInventarioPendiente movimiento : movimientosPendientes) {
            inventarioService.registrarSalidaPorDevolucionCompra(
                    movimiento.producto(),
                    movimiento.cantidad(),
                    movimiento.stockAnterior(),
                    movimiento.stockNuevo(),
                    devolucionGuardada.getId(),
                    movimiento.costoUnitario(),
                    movimiento.valorInventarioAnterior(),
                    movimiento.valorInventarioNuevo()
            );
        }

        auditoriaService.registrar(
                "DEVOLUCION_COMPRA_REGISTRADA",
                "COMPRA",
                compra.getId(),
                "Devolucion: " + devolucionGuardada.getId() + ", total: " + devolucionGuardada.getTotal()
        );

        return DevolucionCompraResponse.desdeEntidad(devolucionGuardada);
    }

    private void validarRequest(RegistrarDevolucionCompraRequest request) {
        if (request == null) {
            throw new BusinessException("Los datos de la devolucion son obligatorios");
        }

        if (request.motivo() == null || request.motivo().isBlank()) {
            throw new BusinessException("El motivo de la devolucion es obligatorio");
        }

        if (request.detalles() == null || request.detalles().isEmpty()) {
            throw new BusinessException("La devolucion debe tener al menos un detalle");
        }
    }

    private Map<Long, Integer> consolidarDetalles(List<DevolucionCompraDetalleRequest> detalles) {
        Map<Long, Integer> cantidadesPorDetalle = new TreeMap<>();

        for (DevolucionCompraDetalleRequest detalle : detalles) {
            if (detalle == null) {
                throw new BusinessException("El detalle de devolucion es obligatorio");
            }

            if (detalle.compraDetalleId() == null) {
                throw new BusinessException("El detalle de compra es obligatorio");
            }

            if (detalle.cantidad() == null || detalle.cantidad() <= 0) {
                throw new BusinessException("La cantidad debe ser mayor a cero");
            }

            cantidadesPorDetalle.merge(detalle.compraDetalleId(), detalle.cantidad(), this::sumarCantidades);
        }

        return cantidadesPorDetalle;
    }

    private List<DetalleDevolucionCompraValidado> validarDetalles(
            Compra compra,
            Map<Long, Integer> cantidadesPorDetalle
    ) {
        Map<Long, CompraDetalle> detallesCompra = compra.getDetalles().stream()
                .collect(Collectors.toMap(CompraDetalle::getId, detalle -> detalle));
        List<DetalleDevolucionCompraValidado> detallesValidados = new ArrayList<>();

        for (Map.Entry<Long, Integer> entry : cantidadesPorDetalle.entrySet()) {
            CompraDetalle compraDetalle = detallesCompra.get(entry.getKey());
            if (compraDetalle == null) {
                throw new BusinessException("El detalle no pertenece a la compra indicada");
            }

            int cantidadDisponible = cantidadDisponibleParaDevolver(compraDetalle);
            if (entry.getValue() > cantidadDisponible) {
                throw new BusinessException(
                        "La cantidad a devolver supera la cantidad disponible del detalle de compra"
                );
            }

            detallesValidados.add(new DetalleDevolucionCompraValidado(
                    compraDetalle,
                    compraDetalle.getProducto(),
                    entry.getValue()
            ));
        }

        return detallesValidados;
    }

    private int cantidadDisponibleParaDevolver(CompraDetalle compraDetalle) {
        long cantidadDevuelta = devolucionCompraRepository.sumarCantidadDevueltaPorDetalle(compraDetalle.getId());
        long cantidadDisponible = compraDetalle.getCantidad() - cantidadDevuelta;

        if (cantidadDisponible > Integer.MAX_VALUE) {
            throw new BusinessException("La cantidad disponible supera el limite permitido");
        }

        return (int) cantidadDisponible;
    }

    private Map<Long, Producto> buscarProductosParaActualizar(
            List<DetalleDevolucionCompraValidado> detallesValidados
    ) {
        Map<Long, Producto> productos = new LinkedHashMap<>();
        detallesValidados.stream()
                .map(detalle -> detalle.producto().getId())
                .distinct()
                .sorted()
                .forEach(productoId -> productos.put(productoId, productoRepository.findByIdParaActualizar(productoId)
                        .orElseThrow(() -> new RecursoNoEncontradoException(
                                "No se encontro el producto con id: " + productoId
                        ))));

        return productos;
    }

    private Integer sumarCantidades(Integer cantidadActual, Integer cantidadAdicional) {
        try {
            return Math.addExact(cantidadActual, cantidadAdicional);
        } catch (ArithmeticException ex) {
            throw new BusinessException("La cantidad acumulada supera el limite permitido");
        }
    }

    private String normalizarMotivo(String motivo) {
        return motivo.trim();
    }

    private record DetalleDevolucionCompraValidado(
            CompraDetalle compraDetalle,
            Producto producto,
            Integer cantidad
    ) {
    }

    private record MovimientoInventarioPendiente(
            Producto producto,
            Integer cantidad,
            Integer stockAnterior,
            Integer stockNuevo,
            BigDecimal costoUnitario,
            BigDecimal valorInventarioAnterior,
            BigDecimal valorInventarioNuevo
    ) {
    }
}
