package com.agroempresa.erp.catalogo.producto;

import com.agroempresa.erp.catalogo.categoria.Categoria;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductoTest {

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
