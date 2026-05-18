package com.agroempresa.erp.comercial.compra.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CompraDetalleRequest(
        @NotNull(message = "El producto es obligatorio")
        Long productoId,

        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 1, message = "La cantidad debe ser mayor a cero")
        Integer cantidad,

        @NotNull(message = "El costo unitario es obligatorio")
        @DecimalMin(value = "0.01", inclusive = true, message = "El costo unitario debe ser mayor a cero")
        @Digits(integer = 10, fraction = 2, message = "El costo unitario debe tener como máximo 10 enteros y 2 decimales")
        BigDecimal costoUnitario
) {
}
