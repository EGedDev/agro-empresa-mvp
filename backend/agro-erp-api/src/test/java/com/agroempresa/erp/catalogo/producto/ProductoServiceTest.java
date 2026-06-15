package com.agroempresa.erp.catalogo.producto;

import com.agroempresa.erp.catalogo.categoria.Categoria;
import com.agroempresa.erp.catalogo.categoria.CategoriaRepository;
import com.agroempresa.erp.catalogo.producto.dto.ActualizarProductoRequest;
import com.agroempresa.erp.catalogo.producto.dto.ProductoResponse;
import com.agroempresa.erp.common.media.MediaProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private CategoriaRepository categoriaRepository;

    @Mock
    private MediaProperties mediaProperties;

    @InjectMocks
    private ProductoService productoService;

    @Test
    void actualizarMantieneStockActual() {
        Categoria categoriaActual = new Categoria("Fertilizantes", null);
        Categoria categoriaNueva = new Categoria("Insumos", null);
        ReflectionTestUtils.setField(categoriaNueva, "id", 1L);
        Producto producto = crearProducto(categoriaActual, 10);
        ActualizarProductoRequest request = new ActualizarProductoRequest(
                "Urea premium",
                "Granulada",
                new BigDecimal("125.00"),
                3,
                1L,
                null,
                null,
                null,
                true,
                false,
                0
        );

        when(productoRepository.findByIdParaActualizar(10L)).thenReturn(Optional.of(producto));
        when(productoRepository.existsByNombreIgnoreCaseAndIdNot("Urea premium", 10L)).thenReturn(false);
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoriaNueva));

        ProductoResponse response = productoService.actualizar(10L, request);

        assertThat(response.nombre()).isEqualTo("Urea premium");
        assertThat(response.descripcion()).isEqualTo("Granulada");
        assertThat(response.precioVenta()).isEqualByComparingTo(new BigDecimal("125.00"));
        assertThat(response.stockActual()).isEqualTo(10);
        assertThat(response.stockMinimo()).isEqualTo(3);
    }

    private Producto crearProducto(Categoria categoria, Integer stockActual) {
        Producto producto = new Producto(
                "Urea",
                null,
                new BigDecimal("120.00"),
                stockActual,
                2,
                categoria
        );
        ReflectionTestUtils.setField(producto, "id", 10L);
        return producto;
    }
}
