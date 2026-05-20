package com.agroempresa.erp.comercial.compra;

import com.agroempresa.erp.catalogo.producto.Producto;
import com.agroempresa.erp.catalogo.producto.ProductoRepository;
import com.agroempresa.erp.auditoria.AuditoriaService;
import com.agroempresa.erp.comercial.compra.dto.CompraDetalleRequest;
import com.agroempresa.erp.comercial.compra.dto.CompraRequest;
import com.agroempresa.erp.comercial.compra.dto.CompraResponse;
import com.agroempresa.erp.common.error.BusinessException;
import com.agroempresa.erp.common.error.RecursoNoEncontradoException;
import com.agroempresa.erp.common.numeracion.NumeroDocumento;
import com.agroempresa.erp.common.numeracion.NumeracionService;
import com.agroempresa.erp.common.numeracion.TipoDocumento;
import com.agroempresa.erp.common.pagination.PaginaResponse;
import com.agroempresa.erp.common.pagination.Paginacion;
import com.agroempresa.erp.finanzas.EstadoPago;
import com.agroempresa.erp.inventario.InventarioService;
import com.agroempresa.erp.proveedor.Proveedor;
import com.agroempresa.erp.proveedor.ProveedorRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class CompraService {

    private static final Map<String, String> CAMPOS_ORDENABLES = Map.ofEntries(
            Map.entry("id", "id"),
            Map.entry("numero", "numero"),
            Map.entry("fechaCompra", "fechaCompra"),
            Map.entry("fechaVencimiento", "fechaVencimiento"),
            Map.entry("total", "total"),
            Map.entry("totalPagado", "totalPagado"),
            Map.entry("saldoPendiente", "saldoPendiente"),
            Map.entry("estado", "estado"),
            Map.entry("estadoPago", "estadoPago"),
            Map.entry("creadoEn", "creadoEn"),
            Map.entry("actualizadoEn", "actualizadoEn")
    );

    private static final Sort ORDEN_DEFAULT = Sort.by(Sort.Direction.DESC, "fechaCompra");

    private final CompraRepository compraRepository;
    private final ProveedorRepository proveedorRepository;
    private final ProductoRepository productoRepository;
    private final InventarioService inventarioService;
    private final AuditoriaService auditoriaService;
    private final NumeracionService numeracionService;

    public CompraService(
            CompraRepository compraRepository,
            ProveedorRepository proveedorRepository,
            ProductoRepository productoRepository,
            InventarioService inventarioService,
            AuditoriaService auditoriaService,
            NumeracionService numeracionService
    ) {
        this.compraRepository = compraRepository;
        this.proveedorRepository = proveedorRepository;
        this.productoRepository = productoRepository;
        this.inventarioService = inventarioService;
        this.auditoriaService = auditoriaService;
        this.numeracionService = numeracionService;
    }

    @Transactional(readOnly = true)
    public PaginaResponse<CompraResponse> listar(
            String numero,
            Long proveedorId,
            EstadoCompra estado,
            EstadoPago estadoPago,
            LocalDate desde,
            LocalDate hasta,
            Integer pagina,
            Integer tamanio,
            String orden
    ) {
        validarProveedorSiFueInformado(proveedorId);
        validarRangoFechas(desde, hasta);

        return PaginaResponse.desde(
                compraRepository.buscar(
                        NumeroDocumento.normalizarFiltro(numero),
                        proveedorId,
                        estado,
                        estadoPago,
                        inicioDia(desde),
                        inicioDiaPosterior(hasta),
                        Paginacion.crear(pagina, tamanio, orden, CAMPOS_ORDENABLES, ORDEN_DEFAULT)
                ),
                CompraResponse::desdeEntidad
        );
    }

    @Transactional(readOnly = true)
    public CompraResponse obtenerPorId(Long id) {
        Compra compra = buscarCompraPorId(id);
        return CompraResponse.desdeEntidad(compra);
    }

    @Transactional(readOnly = true)
    public PaginaResponse<CompraResponse> listarPorProveedor(
            Long proveedorId,
            Integer pagina,
            Integer tamanio,
            String orden
    ) {
        if (proveedorId == null) {
            throw new BusinessException("El proveedor es obligatorio");
        }

        return listar(null, proveedorId, null, null, null, null, pagina, tamanio, orden);
    }

    @Transactional(readOnly = true)
    public PaginaResponse<CompraResponse> listarPorEstado(
            EstadoCompra estado,
            Integer pagina,
            Integer tamanio,
            String orden
    ) {
        if (estado == null) {
            throw new BusinessException("El estado de la compra es obligatorio");
        }

        return listar(null, null, estado, null, null, null, pagina, tamanio, orden);
    }

    @Transactional
    public CompraResponse crear(CompraRequest request) {
        validarCompraRequest(request);
        validarDetallesRequest(request.detalles());

        Proveedor proveedor = proveedorRepository.findById(request.proveedorId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró el proveedor con id: " + request.proveedorId()
                ));

        validarProveedorActivo(proveedor);

        Map<Long, Producto> productos = buscarProductosParaActualizar(
                extraerProductoIdsOrdenados(request.detalles()),
                true
        );

        Compra compra = new Compra(proveedor, request.fechaVencimiento());
        compra.asignarNumero(numeracionService.generar(TipoDocumento.COMPRA));
        List<MovimientoInventarioPendiente> movimientosPendientes = new ArrayList<>();

        for (CompraDetalleRequest detalleRequest : request.detalles()) {
            Producto producto = productos.get(detalleRequest.productoId());
            Integer stockAnterior = producto.getStockActual();
            BigDecimal costoUnitario = detalleRequest.costoUnitario();
            BigDecimal valorInventarioAnterior = producto.getValorInventario();

            producto.aumentarStockConCosto(detalleRequest.cantidad(), costoUnitario);

            Integer stockNuevo = producto.getStockActual();

            compra.agregarDetalle(
                    producto,
                    detalleRequest.cantidad(),
                    costoUnitario
            );

            movimientosPendientes.add(new MovimientoInventarioPendiente(
                    producto,
                    detalleRequest.cantidad(),
                    stockAnterior,
                    stockNuevo,
                    costoUnitario,
                    valorInventarioAnterior,
                    producto.getValorInventario()
            ));
        }

        Compra compraGuardada = compraRepository.save(compra);

        for (MovimientoInventarioPendiente movimiento : movimientosPendientes) {
            inventarioService.registrarEntradaPorCompra(
                    movimiento.producto(),
                    movimiento.cantidad(),
                    movimiento.stockAnterior(),
                    movimiento.stockNuevo(),
                    compraGuardada.getId(),
                    movimiento.costoUnitario(),
                    movimiento.valorInventarioAnterior(),
                    movimiento.valorInventarioNuevo()
            );
        }

        auditoriaService.registrar(
                "COMPRA_REGISTRADA",
                "COMPRA",
                compraGuardada.getId(),
                "Total: " + compraGuardada.getTotal()
        );

        return CompraResponse.desdeEntidad(compraGuardada);
    }

    @Transactional
    public CompraResponse cancelar(Long id) {
        Compra compra = buscarCompraParaActualizar(id);

        if (compra.getEstado() == EstadoCompra.CANCELADA) {
            throw new BusinessException("La compra ya se encuentra cancelada");
        }

        if (compra.getTotalPagado().compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessException("No se puede cancelar una compra con pagos registrados");
        }

        Map<Long, Integer> cantidadesPorProducto = consolidarCantidadesPorProducto(compra.getDetalles());
        Map<Long, Producto> productos = buscarProductosParaActualizar(
                new ArrayList<>(cantidadesPorProducto.keySet()),
                false
        );

        validarStockParaCancelar(cantidadesPorProducto, productos);

        for (CompraDetalle detalle : compra.getDetalles()) {
            Producto producto = productos.get(detalle.getProducto().getId());
            Integer stockAnterior = producto.getStockActual();
            BigDecimal valorInventarioAnterior = producto.getValorInventario();

            producto.descontarStockConCosto(detalle.getCantidad(), detalle.getCostoUnitario());

            inventarioService.registrarSalidaPorCancelacionCompra(
                    producto,
                    detalle.getCantidad(),
                    stockAnterior,
                    producto.getStockActual(),
                    compra.getId(),
                    detalle.getCostoUnitario(),
                    valorInventarioAnterior,
                    producto.getValorInventario()
            );
        }

        compra.cancelar();

        auditoriaService.registrar(
                "COMPRA_CANCELADA",
                "COMPRA",
                compra.getId(),
                "Total: " + compra.getTotal()
        );

        return CompraResponse.desdeEntidad(compra);
    }

    private Compra buscarCompraPorId(Long id) {
        return compraRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró la compra con id: " + id
                ));
    }

    private Compra buscarCompraParaActualizar(Long id) {
        return compraRepository.findByIdParaActualizar(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró la compra con id: " + id
                ));
    }

    private void validarCompraRequest(CompraRequest request) {
        if (request == null) {
            throw new BusinessException("Los datos de la compra son obligatorios");
        }

        if (request.proveedorId() == null) {
            throw new BusinessException("El proveedor es obligatorio");
        }

        if (request.detalles() == null || request.detalles().isEmpty()) {
            throw new BusinessException("La compra debe tener al menos un producto");
        }
    }

    private void validarDetallesRequest(List<CompraDetalleRequest> detalles) {
        for (CompraDetalleRequest detalle : detalles) {
            if (detalle == null) {
                throw new BusinessException("El detalle de compra es obligatorio");
            }

            if (detalle.productoId() == null) {
                throw new BusinessException("El producto es obligatorio");
            }

            if (detalle.cantidad() == null || detalle.cantidad() <= 0) {
                throw new BusinessException("La cantidad debe ser mayor a cero");
            }

            if (detalle.costoUnitario() == null || detalle.costoUnitario().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("El costo unitario debe ser mayor a cero");
            }
        }
    }

    private void validarProveedorActivo(Proveedor proveedor) {
        if (!proveedor.getActivo()) {
            throw new BusinessException("No se puede registrar una compra para un proveedor inactivo");
        }
    }

    private void validarProveedorSiFueInformado(Long proveedorId) {
        if (proveedorId != null && !proveedorRepository.existsById(proveedorId)) {
            throw new RecursoNoEncontradoException("No se encontró el proveedor con id: " + proveedorId);
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

    private List<Long> extraerProductoIdsOrdenados(List<CompraDetalleRequest> detalles) {
        return detalles.stream()
                .map(CompraDetalleRequest::productoId)
                .distinct()
                .sorted()
                .toList();
    }

    private Map<Long, Producto> buscarProductosParaActualizar(List<Long> productoIds, boolean validarActivo) {
        Map<Long, Producto> productos = new LinkedHashMap<>();

        for (Long productoId : productoIds) {
            Producto producto = productoRepository.findByIdParaActualizar(productoId)
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "No se encontró el producto con id: " + productoId
                    ));

            if (validarActivo && !producto.getActivo()) {
                throw new BusinessException("El producto " + producto.getNombre() + " se encuentra inactivo");
            }

            productos.put(productoId, producto);
        }

        return productos;
    }

    private Map<Long, Integer> consolidarCantidadesPorProducto(List<CompraDetalle> detalles) {
        Map<Long, Integer> cantidadesPorProducto = new TreeMap<>();

        for (CompraDetalle detalle : detalles) {
            cantidadesPorProducto.merge(
                    detalle.getProducto().getId(),
                    detalle.getCantidad(),
                    this::sumarCantidades
            );
        }

        return cantidadesPorProducto;
    }

    private Integer sumarCantidades(Integer cantidadActual, Integer cantidadAdicional) {
        try {
            return Math.addExact(cantidadActual, cantidadAdicional);
        } catch (ArithmeticException ex) {
            throw new BusinessException("La cantidad acumulada supera el límite permitido");
        }
    }

    private void validarStockParaCancelar(
            Map<Long, Integer> cantidadesPorProducto,
            Map<Long, Producto> productos
    ) {
        for (Map.Entry<Long, Integer> entry : cantidadesPorProducto.entrySet()) {
            Producto producto = productos.get(entry.getKey());

            if (producto.getStockActual() < entry.getValue()) {
                throw new BusinessException(
                        "Stock insuficiente para cancelar la compra del producto: " + producto.getNombre()
                );
            }
        }
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
