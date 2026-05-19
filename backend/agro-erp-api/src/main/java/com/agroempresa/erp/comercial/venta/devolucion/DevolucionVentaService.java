package com.agroempresa.erp.comercial.venta.devolucion;

import com.agroempresa.erp.auditoria.AuditoriaService;
import com.agroempresa.erp.catalogo.producto.Producto;
import com.agroempresa.erp.catalogo.producto.ProductoRepository;
import com.agroempresa.erp.comercial.venta.EstadoVenta;
import com.agroempresa.erp.comercial.venta.Venta;
import com.agroempresa.erp.comercial.venta.VentaDetalle;
import com.agroempresa.erp.comercial.venta.VentaRepository;
import com.agroempresa.erp.comercial.venta.devolucion.dto.DevolucionVentaDetalleRequest;
import com.agroempresa.erp.comercial.venta.devolucion.dto.DevolucionVentaResponse;
import com.agroempresa.erp.comercial.venta.devolucion.dto.RegistrarDevolucionVentaRequest;
import com.agroempresa.erp.common.error.BusinessException;
import com.agroempresa.erp.common.error.RecursoNoEncontradoException;
import com.agroempresa.erp.common.numeracion.NumeracionService;
import com.agroempresa.erp.common.numeracion.TipoDocumento;
import com.agroempresa.erp.common.pagination.PaginaResponse;
import com.agroempresa.erp.common.pagination.Paginacion;
import com.agroempresa.erp.inventario.InventarioService;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class DevolucionVentaService {

    private static final Map<String, String> CAMPOS_ORDENABLES = Map.of(
            "id", "id",
            "numero", "numero",
            "fechaDevolucion", "fechaDevolucion",
            "total", "total",
            "creadoEn", "creadoEn"
    );

    private static final Sort ORDEN_DEFAULT = Sort.by(Sort.Direction.DESC, "fechaDevolucion");

    private final DevolucionVentaRepository devolucionVentaRepository;
    private final VentaRepository ventaRepository;
    private final ProductoRepository productoRepository;
    private final InventarioService inventarioService;
    private final AuditoriaService auditoriaService;
    private final NumeracionService numeracionService;

    public DevolucionVentaService(
            DevolucionVentaRepository devolucionVentaRepository,
            VentaRepository ventaRepository,
            ProductoRepository productoRepository,
            InventarioService inventarioService,
            AuditoriaService auditoriaService,
            NumeracionService numeracionService
    ) {
        this.devolucionVentaRepository = devolucionVentaRepository;
        this.ventaRepository = ventaRepository;
        this.productoRepository = productoRepository;
        this.inventarioService = inventarioService;
        this.auditoriaService = auditoriaService;
        this.numeracionService = numeracionService;
    }

    @Transactional(readOnly = true)
    public PaginaResponse<DevolucionVentaResponse> listarPorVenta(
            Long ventaId,
            Integer pagina,
            Integer tamanio,
            String orden
    ) {
        if (ventaId == null) {
            throw new BusinessException("La venta es obligatoria");
        }

        if (!ventaRepository.existsById(ventaId)) {
            throw new RecursoNoEncontradoException("No se encontro la venta con id: " + ventaId);
        }

        return PaginaResponse.desde(
                devolucionVentaRepository.buscarPorVenta(
                        ventaId,
                        Paginacion.crear(pagina, tamanio, orden, CAMPOS_ORDENABLES, ORDEN_DEFAULT)
                ),
                DevolucionVentaResponse::desdeEntidad
        );
    }

    @Transactional
    public DevolucionVentaResponse registrar(Long ventaId, RegistrarDevolucionVentaRequest request) {
        if (ventaId == null) {
            throw new BusinessException("La venta es obligatoria");
        }

        validarRequest(request);

        Venta venta = ventaRepository.findByIdParaActualizar(ventaId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontro la venta con id: " + ventaId
                ));

        if (venta.getEstado() == EstadoVenta.CANCELADA) {
            throw new BusinessException("No se puede registrar devoluciones para una venta cancelada");
        }

        Map<Long, Integer> cantidadesPorDetalle = consolidarDetalles(request.detalles());
        List<DetalleDevolucionVentaValidado> detallesValidados = validarDetalles(venta, cantidadesPorDetalle);
        Map<Long, Producto> productos = buscarProductosParaActualizar(detallesValidados);

        DevolucionVenta devolucion = new DevolucionVenta(venta, normalizarMotivo(request.motivo()));
        devolucion.asignarNumero(numeracionService.generar(TipoDocumento.DEVOLUCION_VENTA));
        List<MovimientoInventarioPendiente> movimientosPendientes = new ArrayList<>();

        for (DetalleDevolucionVentaValidado detalleValidado : detallesValidados) {
            Producto producto = productos.get(detalleValidado.producto().getId());
            Integer stockAnterior = producto.getStockActual();

            producto.aumentarStock(detalleValidado.cantidad());
            devolucion.agregarDetalle(detalleValidado.ventaDetalle(), detalleValidado.cantidad());

            movimientosPendientes.add(new MovimientoInventarioPendiente(
                    producto,
                    detalleValidado.cantidad(),
                    stockAnterior,
                    producto.getStockActual()
            ));
        }

        try {
            venta.registrarDevolucion(devolucion.getTotal());
        } catch (IllegalArgumentException | IllegalStateException ex) {
            throw new BusinessException(ex.getMessage());
        }

        DevolucionVenta devolucionGuardada = devolucionVentaRepository.save(devolucion);

        for (MovimientoInventarioPendiente movimiento : movimientosPendientes) {
            inventarioService.registrarEntradaPorDevolucionVenta(
                    movimiento.producto(),
                    movimiento.cantidad(),
                    movimiento.stockAnterior(),
                    movimiento.stockNuevo(),
                    devolucionGuardada.getId()
            );
        }

        auditoriaService.registrar(
                "DEVOLUCION_VENTA_REGISTRADA",
                "VENTA",
                venta.getId(),
                "Devolucion: " + devolucionGuardada.getId() + ", total: " + devolucionGuardada.getTotal()
        );

        return DevolucionVentaResponse.desdeEntidad(devolucionGuardada);
    }

    private void validarRequest(RegistrarDevolucionVentaRequest request) {
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

    private Map<Long, Integer> consolidarDetalles(List<DevolucionVentaDetalleRequest> detalles) {
        Map<Long, Integer> cantidadesPorDetalle = new TreeMap<>();

        for (DevolucionVentaDetalleRequest detalle : detalles) {
            if (detalle == null) {
                throw new BusinessException("El detalle de devolucion es obligatorio");
            }

            if (detalle.ventaDetalleId() == null) {
                throw new BusinessException("El detalle de venta es obligatorio");
            }

            if (detalle.cantidad() == null || detalle.cantidad() <= 0) {
                throw new BusinessException("La cantidad debe ser mayor a cero");
            }

            cantidadesPorDetalle.merge(detalle.ventaDetalleId(), detalle.cantidad(), this::sumarCantidades);
        }

        return cantidadesPorDetalle;
    }

    private List<DetalleDevolucionVentaValidado> validarDetalles(
            Venta venta,
            Map<Long, Integer> cantidadesPorDetalle
    ) {
        Map<Long, VentaDetalle> detallesVenta = venta.getDetalles().stream()
                .collect(Collectors.toMap(VentaDetalle::getId, detalle -> detalle));
        List<DetalleDevolucionVentaValidado> detallesValidados = new ArrayList<>();

        for (Map.Entry<Long, Integer> entry : cantidadesPorDetalle.entrySet()) {
            VentaDetalle ventaDetalle = detallesVenta.get(entry.getKey());
            if (ventaDetalle == null) {
                throw new BusinessException("El detalle no pertenece a la venta indicada");
            }

            int cantidadDisponible = cantidadDisponibleParaDevolver(ventaDetalle);
            if (entry.getValue() > cantidadDisponible) {
                throw new BusinessException(
                        "La cantidad a devolver supera la cantidad disponible del detalle de venta"
                );
            }

            detallesValidados.add(new DetalleDevolucionVentaValidado(
                    ventaDetalle,
                    ventaDetalle.getProducto(),
                    entry.getValue()
            ));
        }

        return detallesValidados;
    }

    private int cantidadDisponibleParaDevolver(VentaDetalle ventaDetalle) {
        long cantidadDevuelta = devolucionVentaRepository.sumarCantidadDevueltaPorDetalle(ventaDetalle.getId());
        long cantidadDisponible = ventaDetalle.getCantidad() - cantidadDevuelta;

        if (cantidadDisponible > Integer.MAX_VALUE) {
            throw new BusinessException("La cantidad disponible supera el limite permitido");
        }

        return (int) cantidadDisponible;
    }

    private Map<Long, Producto> buscarProductosParaActualizar(
            List<DetalleDevolucionVentaValidado> detallesValidados
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

    private record DetalleDevolucionVentaValidado(
            VentaDetalle ventaDetalle,
            Producto producto,
            Integer cantidad
    ) {
    }

    private record MovimientoInventarioPendiente(
            Producto producto,
            Integer cantidad,
            Integer stockAnterior,
            Integer stockNuevo
    ) {
    }
}
