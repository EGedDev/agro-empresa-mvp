package com.agroempresa.erp.comercial.venta;

import com.agroempresa.erp.catalogo.producto.Producto;
import com.agroempresa.erp.catalogo.producto.ProductoRepository;
import com.agroempresa.erp.cliente.Cliente;
import com.agroempresa.erp.cliente.ClienteRepository;
import com.agroempresa.erp.comercial.venta.dto.VentaDetalleRequest;
import com.agroempresa.erp.comercial.venta.dto.VentaRequest;
import com.agroempresa.erp.comercial.venta.dto.VentaResponse;
import com.agroempresa.erp.common.error.BusinessException;
import com.agroempresa.erp.common.error.RecursoNoEncontradoException;
import com.agroempresa.erp.inventario.InventarioService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class VentaService {

    private final VentaRepository ventaRepository;
    private final ClienteRepository clienteRepository;
    private final ProductoRepository productoRepository;
    private final InventarioService inventarioService;

    public VentaService(
            VentaRepository ventaRepository,
            ClienteRepository clienteRepository,
            ProductoRepository productoRepository,
            InventarioService inventarioService
    ) {
        this.ventaRepository = ventaRepository;
        this.clienteRepository = clienteRepository;
        this.productoRepository = productoRepository;
        this.inventarioService = inventarioService;
    }

    @Transactional(readOnly = true)
    public List<VentaResponse> listar() {
        return ventaRepository.findAllByOrderByFechaVentaDesc()
                .stream()
                .map(VentaResponse::desdeEntidad)
                .toList();
    }

    @Transactional(readOnly = true)
    public VentaResponse obtenerPorId(Long id) {
        Venta venta = buscarVentaPorId(id);
        return VentaResponse.desdeEntidad(venta);
    }

    @Transactional(readOnly = true)
    public List<VentaResponse> listarPorCliente(Long clienteId) {
        return ventaRepository.findByClienteIdOrderByFechaVentaDesc(clienteId)
                .stream()
                .map(VentaResponse::desdeEntidad)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<VentaResponse> listarPorEstado(EstadoVenta estado) {
        return ventaRepository.findByEstadoOrderByFechaVentaDesc(estado)
                .stream()
                .map(VentaResponse::desdeEntidad)
                .toList();
    }

    @Transactional
    public VentaResponse crear(VentaRequest request) {
        validarVentaRequest(request);

        Cliente cliente = clienteRepository.findById(request.clienteId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró el cliente con id: " + request.clienteId()
                ));

        Venta venta = new Venta(cliente);

        List<MovimientoInventarioPendiente> movimientosPendientes = new ArrayList<>();

        for (VentaDetalleRequest detalleRequest : request.detalles()) {
            Producto producto = productoRepository.findById(detalleRequest.productoId())
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "No se encontró el producto con id: " + detalleRequest.productoId()
                    ));

            validarDetalleVenta(producto, detalleRequest);

            Integer stockAnterior = producto.getStockActual();

            producto.descontarStock(detalleRequest.cantidad());

            Integer stockNuevo = producto.getStockActual();

            venta.agregarDetalle(producto, detalleRequest.cantidad());

            movimientosPendientes.add(new MovimientoInventarioPendiente(
                    producto,
                    detalleRequest.cantidad(),
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

        return VentaResponse.desdeEntidad(ventaGuardada);
    }

    @Transactional
    public VentaResponse cancelar(Long id) {
        Venta venta = buscarVentaPorId(id);

        if (venta.getEstado() == EstadoVenta.CANCELADA) {
            throw new BusinessException("La venta ya se encuentra cancelada");
        }

        for (VentaDetalle detalle : venta.getDetalles()) {
            Producto producto = detalle.getProducto();
            producto.aumentarStock(detalle.getCantidad());
        }

        venta.cancelar();

        return VentaResponse.desdeEntidad(venta);
    }

    private Venta buscarVentaPorId(Long id) {
        return ventaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró la venta con id: " + id
                ));
    }

    private void validarVentaRequest(VentaRequest request) {
        if (request.clienteId() == null) {
            throw new BusinessException("El cliente es obligatorio");
        }

        if (request.detalles() == null || request.detalles().isEmpty()) {
            throw new BusinessException("La venta debe tener al menos un producto");
        }
    }

    private void validarDetalleVenta(Producto producto, VentaDetalleRequest detalleRequest) {
        if (!producto.getActivo()) {
            throw new BusinessException("El producto " + producto.getNombre() + " se encuentra inactivo");
        }

        if (detalleRequest.cantidad() == null || detalleRequest.cantidad() <= 0) {
            throw new BusinessException("La cantidad debe ser mayor a cero");
        }

        if (producto.getStockActual() < detalleRequest.cantidad()) {
            throw new BusinessException(
                    "Stock insuficiente para el producto: " + producto.getNombre()
            );
        }
    }

    private record MovimientoInventarioPendiente(
            Producto producto,
            Integer cantidad,
            Integer stockAnterior,
            Integer stockNuevo
    ) {
    }
}