package com.agroempresa.erp.comercial.venta;

import com.agroempresa.erp.catalogo.categoria.Categoria;
import com.agroempresa.erp.catalogo.producto.Producto;
import com.agroempresa.erp.catalogo.producto.ProductoRepository;
import com.agroempresa.erp.auditoria.AuditoriaService;
import com.agroempresa.erp.cliente.Cliente;
import com.agroempresa.erp.cliente.ClienteRepository;
import com.agroempresa.erp.comercial.venta.dto.VentaDetalleRequest;
import com.agroempresa.erp.comercial.venta.dto.VentaRequest;
import com.agroempresa.erp.comercial.venta.dto.VentaResponse;
import com.agroempresa.erp.common.error.BusinessException;
import com.agroempresa.erp.common.numeracion.NumeracionService;
import com.agroempresa.erp.common.numeracion.TipoDocumento;
import com.agroempresa.erp.finanzas.EstadoPago;
import com.agroempresa.erp.inventario.InventarioService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VentaServiceTest {

    @Mock
    private VentaRepository ventaRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private InventarioService inventarioService;

    @Mock
    private AuditoriaService auditoriaService;

    @Mock
    private NumeracionService numeracionService;

    @InjectMocks
    private VentaService ventaService;

    @Test
    void crearVentaConsolidaProductosRepetidosYRegistraUnMovimientoDeInventario() {
        Cliente cliente = new Cliente("Agro Cliente", "12345678", null, null, null);
        Producto producto = crearProductoConStock(10);
        ReflectionTestUtils.setField(producto, "id", 2L);
        VentaRequest request = new VentaRequest(
                1L,
                List.of(
                        new VentaDetalleRequest(2L, 2),
                        new VentaDetalleRequest(2L, 3)
                )
        );

        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(numeracionService.generar(TipoDocumento.VENTA)).thenReturn("V-000001");
        when(productoRepository.findByIdParaActualizar(2L)).thenReturn(Optional.of(producto));
        when(ventaRepository.save(any(Venta.class))).thenAnswer(invocation -> {
            Venta venta = invocation.getArgument(0);
            ReflectionTestUtils.setField(venta, "id", 15L);
            return venta;
        });

        VentaResponse response = ventaService.crear(request);

        assertThat(producto.getStockActual()).isEqualTo(5);
        assertThat(response.numero()).isEqualTo("V-000001");
        assertThat(response.detalles()).hasSize(1);
        assertThat(response.detalles().getFirst().productoId()).isEqualTo(2L);
        assertThat(response.detalles().getFirst().cantidad()).isEqualTo(5);
        assertThat(response.total()).isEqualByComparingTo(BigDecimal.valueOf(600));
        assertThat(response.totalPagado()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.saldoPendiente()).isEqualByComparingTo(BigDecimal.valueOf(600));
        assertThat(response.estadoPago()).isEqualTo(EstadoPago.PENDIENTE);
        assertThat(response.fechaVencimiento()).isEqualTo(LocalDate.now());

        verify(inventarioService).registrarSalidaPorVenta(
                producto,
                5,
                10,
                5,
                15L
        );
    }

    @Test
    void crearVentaRechazaClienteInactivo() {
        Cliente cliente = new Cliente("Agro Cliente", "12345678", null, null, null);
        cliente.desactivar();
        VentaRequest request = new VentaRequest(
                1L,
                List.of(new VentaDetalleRequest(2L, 1))
        );

        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));

        assertThatThrownBy(() -> ventaService.crear(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("No se puede registrar una venta para un cliente inactivo");

        verifyNoInteractions(productoRepository, inventarioService);
    }

    @Test
    void crearVentaValidaTodosLosProductosAntesDeDescontarStock() {
        Cliente cliente = new Cliente("Agro Cliente", "12345678", null, null, null);
        Producto productoConStock = crearProductoConStock(10);
        Producto productoSinStock = crearProductoConStock(1);
        VentaRequest request = new VentaRequest(
                1L,
                List.of(
                        new VentaDetalleRequest(2L, 2),
                        new VentaDetalleRequest(3L, 5)
                )
        );

        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(productoRepository.findByIdParaActualizar(2L)).thenReturn(Optional.of(productoConStock));
        when(productoRepository.findByIdParaActualizar(3L)).thenReturn(Optional.of(productoSinStock));

        assertThatThrownBy(() -> ventaService.crear(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Stock insuficiente para el producto: Urea");

        assertThat(productoConStock.getStockActual()).isEqualTo(10);
        assertThat(productoSinStock.getStockActual()).isEqualTo(1);
        verifyNoInteractions(ventaRepository, inventarioService);
    }

    @Test
    void cancelarVentaRestauraStockYRegistraEntradaDeInventario() {
        Producto producto = crearProductoConStock(5);
        ReflectionTestUtils.setField(producto, "id", 20L);
        Venta venta = crearVentaConDetalle(producto, 2);
        ReflectionTestUtils.setField(venta, "id", 10L);

        when(ventaRepository.findByIdParaActualizar(10L)).thenReturn(Optional.of(venta));
        when(productoRepository.findByIdParaActualizar(20L)).thenReturn(Optional.of(producto));

        VentaResponse response = ventaService.cancelar(10L);

        assertThat(producto.getStockActual()).isEqualTo(7);
        assertThat(response.estado()).isEqualTo(EstadoVenta.CANCELADA);
        assertThat(response.estadoPago()).isEqualTo(EstadoPago.CANCELADA);

        verify(inventarioService).registrarEntradaPorCancelacionVenta(
                producto,
                2,
                5,
                7,
                10L
        );
    }

    @Test
    void cancelarVentaRechazaVentasConPagosRegistrados() {
        Producto producto = crearProductoConStock(5);
        Venta venta = crearVentaConDetalle(producto, 2);
        venta.registrarPago(new BigDecimal("10.00"));
        ReflectionTestUtils.setField(venta, "id", 10L);

        when(ventaRepository.findByIdParaActualizar(10L)).thenReturn(Optional.of(venta));

        assertThatThrownBy(() -> ventaService.cancelar(10L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("No se puede cancelar una venta con pagos registrados");

        verifyNoInteractions(inventarioService);
    }

    private Venta crearVentaConDetalle(Producto producto, Integer cantidad) {
        Cliente cliente = new Cliente("Agro Cliente", "12345678", null, null, null);
        Venta venta = new Venta(cliente);
        venta.agregarDetalle(producto, cantidad);

        return venta;
    }

    private Producto crearProductoConStock(Integer stockActual) {
        Categoria categoria = new Categoria("Fertilizantes", null);

        return new Producto(
                "Urea",
                null,
                BigDecimal.valueOf(120),
                stockActual,
                2,
                categoria
        );
    }
}
