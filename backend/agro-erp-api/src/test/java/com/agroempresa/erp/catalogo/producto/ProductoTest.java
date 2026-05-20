package com.agroempresa.erp.catalogo.producto;

import com.agroempresa.erp.catalogo.categoria.Categoria;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductoTest {

    @Test
    void aumentarStockConCostoRecalculaCostoPromedioPonderado() {
        Producto producto = new Producto(
                "Urea",
                null,
                BigDecimal.valueOf(120),
                10,
                2,
                new Categoria("Fertilizantes", null),
                new BigDecimal("4.00")
        );

        producto.aumentarStockConCosto(5, new BigDecimal("6.00"));

        assertThat(producto.getStockActual()).isEqualTo(15);
        assertThat(producto.getValorInventario()).isEqualByComparingTo(new BigDecimal("70.00"));
        assertThat(producto.getCostoPromedio()).isEqualByComparingTo(new BigDecimal("4.6667"));
    }

    @Test
    void descontarStockConCostoReduceValorInventarioSinVolverloNegativo() {
        Producto producto = new Producto(
                "Urea",
                null,
                BigDecimal.valueOf(120),
                10,
                2,
                new Categoria("Fertilizantes", null),
                new BigDecimal("8.50")
        );

        producto.descontarStockConCosto(3, new BigDecimal("8.50"));

        assertThat(producto.getStockActual()).isEqualTo(7);
        assertThat(producto.getValorInventario()).isEqualByComparingTo(new BigDecimal("59.50"));
        assertThat(producto.getCostoPromedio()).isEqualByComparingTo(new BigDecimal("8.5000"));
    }

    @Test
    void aumentarStockRechazaOverflow() {
        Producto producto = new Producto(
                "Urea",
                null,
                BigDecimal.valueOf(120),
                Integer.MAX_VALUE,
                2,
                new Categoria("Fertilizantes", null)
        );

        assertThatThrownBy(() -> producto.aumentarStock(1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El stock supera el límite permitido");
    }
}
