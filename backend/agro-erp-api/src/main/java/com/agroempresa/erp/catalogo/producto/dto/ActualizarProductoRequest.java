package com.agroempresa.erp.catalogo.producto.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ActualizarProductoRequest(

        @NotBlank(message = "El nombre del producto es obligatorio")
        @Size(max = 160, message = "El nombre del producto no debe superar los 160 caracteres")
        String nombre,

        @Size(max = 500, message = "La descripcion no debe superar los 500 caracteres")
        String descripcion,

        @NotNull(message = "El precio de venta es obligatorio")
        @DecimalMin(value = "0.00", inclusive = true, message = "El precio de venta no puede ser negativo")
        @Digits(integer = 10, fraction = 2, message = "El precio de venta debe tener como maximo 10 enteros y 2 decimales")
        BigDecimal precioVenta,

        @NotNull(message = "El stock minimo es obligatorio")
        @Min(value = 0, message = "El stock minimo no puede ser negativo")
        Integer stockMinimo,

        @NotNull(message = "La categoria es obligatoria")
        Long categoriaId,

        @Size(max = 500, message = "La URL de imagen no debe superar los 500 caracteres")
        String imagenUrl,

        @Size(max = 160, message = "El texto alternativo no debe superar los 160 caracteres")
        String imagenAlt,

        @Size(max = 700, message = "El resumen comercial no debe superar los 700 caracteres")
        String resumenComercial,

        String descripcionWeb,
        String informacionAdicional,
        @Size(max = 300) String ingredienteActivo,
        @Size(max = 500) String composicion,
        @Size(max = 300) String formulacion,
        @Size(max = 200) String numeroRegistro,
        @Size(max = 500) String presentaciones,
        @Size(max = 700) String cultivos,
        String modoUso,
        @Size(max = 500) String fichaTecnicaUrl,

        Boolean visibleWeb,

        Boolean destacado,

        Integer ordenWeb
) {
    public ActualizarProductoRequest(
            String nombre, String descripcion, BigDecimal precioVenta, Integer stockMinimo, Long categoriaId,
            String imagenUrl, String imagenAlt, String resumenComercial,
            Boolean visibleWeb, Boolean destacado, Integer ordenWeb
    ) {
        this(nombre, descripcion, precioVenta, stockMinimo, categoriaId, imagenUrl, imagenAlt, resumenComercial,
                null, null, null, null, null, null, null, null, null, null, visibleWeb, destacado, ordenWeb);
    }
}
