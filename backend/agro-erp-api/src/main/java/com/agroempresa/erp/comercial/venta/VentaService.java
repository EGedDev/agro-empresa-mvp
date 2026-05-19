package com.agroempresa.erp.comercial.venta;

import com.agroempresa.erp.catalogo.producto.Producto;
import com.agroempresa.erp.catalogo.producto.ProductoRepository;
import com.agroempresa.erp.auditoria.AuditoriaService;
import com.agroempresa.erp.cliente.Cliente;
import com.agroempresa.erp.cliente.ClienteRepository;
import com.agroempresa.erp.comercial.venta.dto.VentaDetalleRequest;
import com.agroempresa.erp.comercial.venta.dto.VentaRequest;
import com.agroempresa.erp.comercial.venta.dto.VentaResponse;
import com.agroempresa.erp.common.error.BusinessException;
import com.agroempresa.erp.common.error.RecursoNoEncontradoException;
import com.agroempresa.erp.common.numeracion.NumeracionService;
import com.agroempresa.erp.common.numeracion.TipoDocumento;
import com.agroempresa.erp.common.pagination.PaginaResponse;
import com.agroempresa.erp.common.pagination.Paginacion;
import com.agroempresa.erp.finanzas.EstadoPago;
import com.agroempresa.erp.inventario.InventarioService;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class VentaService {

    private static final Map<String, String> CAMPOS_ORDENABLES = Map.ofEntries(
            Map.entry("id", "id"),
            Map.entry("numero", "numero"),
            Map.entry("fechaVenta", "fechaVenta"),
            Map.entry("fechaVencimiento", "fechaVencimiento"),
            Map.entry("total", "total"),
            Map.entry("totalPagado", "totalPagado"),
            Map.entry("saldoPendiente", "saldoPendiente"),
            Map.entry("estado", "estado"),
            Map.entry("estadoPago", "estadoPago"),
            Map.entry("creadoEn", "creadoEn"),
            Map.entry("actualizadoEn", "actualizadoEn")
    );

    private static final Sort ORDEN_DEFAULT = Sort.by(Sort.Direction.DESC, "fechaVenta");

    private final VentaRepository ventaRepository;
    private final ClienteRepository clienteRepository;
    private final ProductoRepository productoRepository;
    private final InventarioService inventarioService;
    private final AuditoriaService auditoriaService;
    private final NumeracionService numeracionService;

    public VentaService(
            VentaRepository ventaRepository,
            ClienteRepository clienteRepository,
            ProductoRepository productoRepository,
            InventarioService inventarioService,
            AuditoriaService auditoriaService,
            NumeracionService numeracionService
    ) {
        this.ventaRepository = ventaRepository;
        this.clienteRepository = clienteRepository;
        this.productoRepository = productoRepository;
        this.inventarioService = inventarioService;
        this.auditoriaService = auditoriaService;
        this.numeracionService = numeracionService;
    }

    @Transactional(readOnly = true)
    public PaginaResponse<VentaResponse> listar(
            Long clienteId,
            EstadoVenta estado,
            EstadoPago estadoPago,
            LocalDate desde,
            LocalDate hasta,
            Integer pagina,
            Integer tamanio,
            String orden
    ) {
        validarClienteSiFueInformado(clienteId);
        validarRangoFechas(desde, hasta);

        return PaginaResponse.desde(
                ventaRepository.buscar(
                        clienteId,
                        estado,
                        estadoPago,
                        inicioDia(desde),
                        inicioDiaPosterior(hasta),
                        Paginacion.crear(pagina, tamanio, orden, CAMPOS_ORDENABLES, ORDEN_DEFAULT)
                ),
                VentaResponse::desdeEntidad
        );
    }

    @Transactional(readOnly = true)
    public VentaResponse obtenerPorId(Long id) {
        Venta venta = buscarVentaPorId(id);
        return VentaResponse.desdeEntidad(venta);
    }

    @Transactional(readOnly = true)
    public PaginaResponse<VentaResponse> listarPorCliente(
            Long clienteId,
            Integer pagina,
            Integer tamanio,
            String orden
    ) {
        if (clienteId == null) {
            throw new BusinessException("El cliente es obligatorio");
        }

        return listar(clienteId, null, null, null, null, pagina, tamanio, orden);
    }

    @Transactional(readOnly = true)
    public PaginaResponse<VentaResponse> listarPorEstado(
            EstadoVenta estado,
            Integer pagina,
            Integer tamanio,
            String orden
    ) {
        if (estado == null) {
            throw new BusinessException("El estado de la venta es obligatorio");
        }

        return listar(null, estado, null, null, null, pagina, tamanio, orden);
    }

    @Transactional
    public VentaResponse crear(VentaRequest request) {
        validarVentaRequest(request);

        Cliente cliente = clienteRepository.findById(request.clienteId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró el cliente con id: " + request.clienteId()
                ));

        validarClienteActivo(cliente);

        Venta venta = new Venta(cliente, request.fechaVencimiento());
        venta.asignarNumero(numeracionService.generar(TipoDocumento.VENTA));

        List<MovimientoInventarioPendiente> movimientosPendientes = new ArrayList<>();
        List<DetalleVentaValidado> detallesValidados = validarDetallesVenta(consolidarDetalles(request.detalles()));

        for (DetalleVentaValidado detalleValidado : detallesValidados) {
            Producto producto = detalleValidado.producto();
            Integer stockAnterior = producto.getStockActual();

            producto.descontarStock(detalleValidado.cantidad());

            Integer stockNuevo = producto.getStockActual();

            venta.agregarDetalle(producto, detalleValidado.cantidad());

            movimientosPendientes.add(new MovimientoInventarioPendiente(
                    producto,
                    detalleValidado.cantidad(),
                    stockAnterior,
                    stockNuevo
            ));
        }

        Venta ventaGuardada = ventaRepository.save(venta);

        for (MovimientoInventarioPendiente movimiento : movimientosPendientes) {
            inventarioService.registrarSalidaPorVenta(
                    movimiento.producto(),
                    movimiento.cantidad(),
                    movimiento.stockAnterior(),
                    movimiento.stockNuevo(),
                    ventaGuardada.getId()
            );
        }

        auditoriaService.registrar(
                "VENTA_REGISTRADA",
                "VENTA",
                ventaGuardada.getId(),
                "Total: " + ventaGuardada.getTotal()
        );

        return VentaResponse.desdeEntidad(ventaGuardada);
    }

    @Transactional
    public VentaResponse cancelar(Long id) {
        Venta venta = buscarVentaParaActualizar(id);

        if (venta.getEstado() == EstadoVenta.CANCELADA) {
            throw new BusinessException("La venta ya se encuentra cancelada");
        }

        if (venta.getTotalPagado().compareTo(java.math.BigDecimal.ZERO) > 0) {
            throw new BusinessException("No se puede cancelar una venta con pagos registrados");
        }

        for (VentaDetalle detalle : venta.getDetalles()) {
            Producto producto = buscarProductoParaActualizar(detalle.getProducto().getId());
            Integer stockAnterior = producto.getStockActual();

            producto.aumentarStock(detalle.getCantidad());

            inventarioService.registrarEntradaPorCancelacionVenta(
                    producto,
                    detalle.getCantidad(),
                    stockAnterior,
                    producto.getStockActual(),
                    venta.getId()
            );
        }

        venta.cancelar();

        auditoriaService.registrar(
                "VENTA_CANCELADA",
                "VENTA",
                venta.getId(),
                "Total: " + venta.getTotal()
        );

        return VentaResponse.desdeEntidad(venta);
    }

    private Venta buscarVentaPorId(Long id) {
        return ventaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró la venta con id: " + id
                ));
    }

    private Venta buscarVentaParaActualizar(Long id) {
        return ventaRepository.findByIdParaActualizar(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró la venta con id: " + id
                ));
    }

    private void validarVentaRequest(VentaRequest request) {
        if (request == null) {
            throw new BusinessException("Los datos de la venta son obligatorios");
        }

        if (request.clienteId() == null) {
            throw new BusinessException("El cliente es obligatorio");
        }

        if (request.detalles() == null || request.detalles().isEmpty()) {
            throw new BusinessException("La venta debe tener al menos un producto");
        }
    }

    private void validarClienteActivo(Cliente cliente) {
        if (!cliente.getActivo()) {
            throw new BusinessException("No se puede registrar una venta para un cliente inactivo");
        }
    }

    private void validarClienteSiFueInformado(Long clienteId) {
        if (clienteId != null && !clienteRepository.existsById(clienteId)) {
            throw new RecursoNoEncontradoException("No se encontró el cliente con id: " + clienteId);
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

    private List<DetalleVentaConsolidado> consolidarDetalles(List<VentaDetalleRequest> detalles) {
        Map<Long, Integer> cantidadesPorProducto = new LinkedHashMap<>();

        for (VentaDetalleRequest detalle : detalles) {
            if (detalle == null) {
                throw new BusinessException("El detalle de venta es obligatorio");
            }

            if (detalle.productoId() == null) {
                throw new BusinessException("El producto es obligatorio");
            }

            if (detalle.cantidad() == null || detalle.cantidad() <= 0) {
                throw new BusinessException("La cantidad debe ser mayor a cero");
            }

            cantidadesPorProducto.merge(detalle.productoId(), detalle.cantidad(), this::sumarCantidades);
        }

        return cantidadesPorProducto.entrySet()
                .stream()
                // Bloquear productos en orden estable reduce el riesgo de deadlocks concurrentes.
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new DetalleVentaConsolidado(entry.getKey(), entry.getValue()))
                .toList();
    }

    private Integer sumarCantidades(Integer cantidadActual, Integer cantidadAdicional) {
        try {
            return Math.addExact(cantidadActual, cantidadAdicional);
        } catch (ArithmeticException ex) {
            throw new BusinessException("La cantidad acumulada supera el límite permitido");
        }
    }

    private List<DetalleVentaValidado> validarDetallesVenta(List<DetalleVentaConsolidado> detallesConsolidados) {
        List<DetalleVentaValidado> detallesValidados = new ArrayList<>();

        for (DetalleVentaConsolidado detalleConsolidado : detallesConsolidados) {
            Producto producto = buscarProductoParaActualizar(detalleConsolidado.productoId());
            validarDetalleVenta(producto, detalleConsolidado.cantidad());
            detallesValidados.add(new DetalleVentaValidado(producto, detalleConsolidado.cantidad()));
        }

        return detallesValidados;
    }

    private Producto buscarProductoParaActualizar(Long productoId) {
        return productoRepository.findByIdParaActualizar(productoId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró el producto con id: " + productoId
                ));
    }

    private void validarDetalleVenta(Producto producto, Integer cantidad) {
        if (!producto.getActivo()) {
            throw new BusinessException("El producto " + producto.getNombre() + " se encuentra inactivo");
        }

        if (producto.getStockActual() < cantidad) {
            throw new BusinessException(
                    "Stock insuficiente para el producto: " + producto.getNombre()
            );
        }
    }

    private record DetalleVentaConsolidado(
            Long productoId,
            Integer cantidad
    ) {
    }

    private record DetalleVentaValidado(
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
