package com.agroempresa.erp.catalogo.producto.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductoRequest(

        @NotBlank(message = "El nombre del producto es obligatorio")
        @Size(max = 160, message = "El nombre del producto no debe superar los 160 caracteres")
        String nombre,

        @Size(max = 500, message = "La descripción no debe superar los 500 caracteres")
        String descripcion,

        @NotNull(message = "El precio de venta es obligatorio")
        @DecimalMin(value = "0.00", inclusive = true, message = "El precio de venta no puede ser negativo")
        @Digits(integer = 10, fraction = 2, message = "El precio de venta debe tener como máximo 10 enteros y 2 decimales")
        BigDecimal precioVenta,

        @NotNull(message = "El stock actual es obligatorio")
        @Min(value = 0, message = "El stock actual no puede ser negativo")
        Integer stockActual,

        @DecimalMin(value = "0.00", inclusive = true, message = "El costo inicial no puede ser negativo")
        @Digits(integer = 10, fraction = 4, message = "El costo inicial debe tener como maximo 10 enteros y 4 decimales")
        BigDecimal costoInicial,

        @NotNull(message = "El stock mínimo es obligatorio")
        @Min(value = 0, message = "El stock mínimo no puede ser negativo")
        Integer stockMinimo,

        @NotNull(message = "La categoría es obligatoria")
        Long categoriaId,

        @Size(max = 500, message = "La URL de imagen no debe superar los 500 caracteres")
        String imagenUrl,

        @Size(max = 160, message = "El texto alternativo no debe superar los 160 caracteres")
        String imagenAlt,

        @Size(max = 700, message = "El resumen comercial no debe superar los 700 caracteres")
        String resumenComercial,

        Boolean visibleWeb,

        Boolean destacado,

        Integer ordenWeb
) {
}
