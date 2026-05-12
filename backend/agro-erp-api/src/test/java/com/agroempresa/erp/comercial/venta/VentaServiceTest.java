package com.agroempresa.erp.comercial.venta;

import com.agroempresa.erp.catalogo.categoria.Categoria;
import com.agroempresa.erp.catalogo.producto.Producto;
import com.agroempresa.erp.catalogo.producto.ProductoRepository;
import com.agroempresa.erp.cliente.Cliente;
import com.agroempresa.erp.cliente.ClienteRepository;
import com.agroempresa.erp.comercial.venta.dto.VentaResponse;
import com.agroempresa.erp.inventario.InventarioService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
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

    @InjectMocks
    private VentaService ventaService;

    @Test
    void cancelarVentaRestauraStockYRegistraEntradaDeInventario() {
        Producto producto = crearProductoConStock(5);
        Venta venta = crearVentaConDetalle(producto, 2);
        ReflectionTestUtils.setField(venta, "id", 10L);

        when(ventaRepository.findById(10L)).thenReturn(Optional.of(venta));

        VentaResponse response = ventaService.cancelar(10L);

        assertThat(producto.getStockActual()).isEqualTo(7);
        assertThat(response.estado()).isEqualTo(EstadoVenta.CANCELADA);

        verify(inventarioService).registrarEntradaPorCancelacionVenta(
                producto,
                2,
                5,
                7,
                10L
        );
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
