package com.agroempresa.erp.comercial.compra;

import com.agroempresa.erp.catalogo.categoria.Categoria;
import com.agroempresa.erp.catalogo.producto.Producto;
import com.agroempresa.erp.catalogo.producto.ProductoRepository;
import com.agroempresa.erp.auditoria.AuditoriaService;
import com.agroempresa.erp.comercial.compra.dto.CompraDetalleRequest;
import com.agroempresa.erp.comercial.compra.dto.CompraRequest;
import com.agroempresa.erp.comercial.compra.dto.CompraResponse;
import com.agroempresa.erp.common.error.BusinessException;
import com.agroempresa.erp.common.numeracion.NumeracionService;
import com.agroempresa.erp.common.numeracion.TipoDocumento;
import com.agroempresa.erp.finanzas.EstadoPago;
import com.agroempresa.erp.inventario.InventarioService;
import com.agroempresa.erp.proveedor.Proveedor;
import com.agroempresa.erp.proveedor.ProveedorRepository;
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
class CompraServiceTest {

    @Mock
    private CompraRepository compraRepository;

    @Mock
    private ProveedorRepository proveedorRepository;

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private InventarioService inventarioService;

    @Mock
    private AuditoriaService auditoriaService;

    @Mock
    private NumeracionService numeracionService;

    @InjectMocks
    private CompraService compraService;

    @Test
    void crearCompraAumentaStockYRegistraEntradaDeInventario() {
        Proveedor proveedor = crearProveedor(1L);
        Producto producto = crearProductoConStock(2L, 4);
        CompraRequest request = new CompraRequest(
                1L,
                List.of(new CompraDetalleRequest(2L, 3, new BigDecimal("30.50")))
        );

        when(proveedorRepository.findById(1L)).thenReturn(Optional.of(proveedor));
        when(numeracionService.generar(TipoDocumento.COMPRA)).thenReturn("C-000001");
        when(productoRepository.findByIdParaActualizar(2L)).thenReturn(Optional.of(producto));
        when(compraRepository.save(any(Compra.class))).thenAnswer(invocation -> {
            Compra compra = invocation.getArgument(0);
            ReflectionTestUtils.setField(compra, "id", 20L);
            return compra;
        });

        CompraResponse response = compraService.crear(request);

        assertThat(producto.getStockActual()).isEqualTo(7);
        assertThat(response.numero()).isEqualTo("C-000001");
        assertThat(response.estado()).isEqualTo(EstadoCompra.REGISTRADA);
        assertThat(response.estadoPago()).isEqualTo(EstadoPago.PENDIENTE);
        assertThat(response.totalPagado()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.saldoPendiente()).isEqualByComparingTo(new BigDecimal("91.50"));
        assertThat(response.detalles()).hasSize(1);
        assertThat(response.total()).isEqualByComparingTo(new BigDecimal("91.50"));
        assertThat(response.fechaVencimiento()).isEqualTo(LocalDate.now());

        verify(inventarioService).registrarEntradaPorCompra(
                producto,
                3,
                4,
                7,
                20L,
                new BigDecimal("30.50"),
                new BigDecimal("40.00"),
                new BigDecimal("131.50")
        );
    }

    @Test
    void crearCompraRechazaProveedorInactivo() {
        Proveedor proveedor = crearProveedor(1L);
        proveedor.desactivar();
        CompraRequest request = new CompraRequest(
                1L,
                List.of(new CompraDetalleRequest(2L, 1, new BigDecimal("10.00")))
        );

        when(proveedorRepository.findById(1L)).thenReturn(Optional.of(proveedor));

        assertThatThrownBy(() -> compraService.crear(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("No se puede registrar una compra para un proveedor inactivo");

        verifyNoInteractions(productoRepository, compraRepository, inventarioService);
    }

    @Test
    void cancelarCompraDescuentaStockYRegistraSalidaDeInventario() {
        Proveedor proveedor = crearProveedor(1L);
        Producto producto = crearProductoConStock(2L, 8);
        Compra compra = crearCompraConDetalle(proveedor, producto, 3, new BigDecimal("10.00"));
        ReflectionTestUtils.setField(compra, "id", 20L);

        when(compraRepository.findByIdParaActualizar(20L)).thenReturn(Optional.of(compra));
        when(productoRepository.findByIdParaActualizar(2L)).thenReturn(Optional.of(producto));

        CompraResponse response = compraService.cancelar(20L);

        assertThat(producto.getStockActual()).isEqualTo(5);
        assertThat(response.estado()).isEqualTo(EstadoCompra.CANCELADA);
        assertThat(response.estadoPago()).isEqualTo(EstadoPago.CANCELADA);

        verify(inventarioService).registrarSalidaPorCancelacionCompra(
                producto,
                3,
                8,
                5,
                20L,
                new BigDecimal("10.00"),
                new BigDecimal("80.00"),
                new BigDecimal("50.00")
        );
    }

    @Test
    void cancelarCompraValidaStockSuficienteAntesDeDescontar() {
        Proveedor proveedor = crearProveedor(1L);
        Producto productoConStock = crearProductoConStock(2L, 5);
        Producto productoSinStock = crearProductoConStock(3L, 1);
        Compra compra = new Compra(proveedor);
        compra.agregarDetalle(productoConStock, 3, new BigDecimal("10.00"));
        compra.agregarDetalle(productoSinStock, 4, new BigDecimal("15.00"));
        ReflectionTestUtils.setField(compra, "id", 20L);

        when(compraRepository.findByIdParaActualizar(20L)).thenReturn(Optional.of(compra));
        when(productoRepository.findByIdParaActualizar(2L)).thenReturn(Optional.of(productoConStock));
        when(productoRepository.findByIdParaActualizar(3L)).thenReturn(Optional.of(productoSinStock));

        assertThatThrownBy(() -> compraService.cancelar(20L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Stock insuficiente para cancelar la compra del producto: Urea");

        assertThat(productoConStock.getStockActual()).isEqualTo(5);
        assertThat(productoSinStock.getStockActual()).isEqualTo(1);
        verifyNoInteractions(inventarioService);
    }

    @Test
    void cancelarCompraRechazaComprasConPagosRegistrados() {
        Proveedor proveedor = crearProveedor(1L);
        Producto producto = crearProductoConStock(2L, 8);
        Compra compra = crearCompraConDetalle(proveedor, producto, 3, new BigDecimal("10.00"));
        compra.registrarPago(new BigDecimal("5.00"));
        ReflectionTestUtils.setField(compra, "id", 20L);

        when(compraRepository.findByIdParaActualizar(20L)).thenReturn(Optional.of(compra));

        assertThatThrownBy(() -> compraService.cancelar(20L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("No se puede cancelar una compra con pagos registrados");

        verifyNoInteractions(productoRepository, inventarioService);
    }

    private Compra crearCompraConDetalle(
            Proveedor proveedor,
            Producto producto,
            Integer cantidad,
            BigDecimal costoUnitario
    ) {
        Compra compra = new Compra(proveedor);
        compra.agregarDetalle(producto, cantidad, costoUnitario);
        return compra;
    }

    private Proveedor crearProveedor(Long id) {
        Proveedor proveedor = new Proveedor("Proveedor Agro", "20123456789", null, null, null);
        ReflectionTestUtils.setField(proveedor, "id", id);
        return proveedor;
    }

    private Producto crearProductoConStock(Long id, Integer stockActual) {
        Categoria categoria = new Categoria("Fertilizantes", null);
        Producto producto = new Producto(
                "Urea",
                null,
                BigDecimal.valueOf(120),
                stockActual,
                2,
                categoria,
                new BigDecimal("10.00")
        );
        ReflectionTestUtils.setField(producto, "id", id);
        return producto;
    }
}
