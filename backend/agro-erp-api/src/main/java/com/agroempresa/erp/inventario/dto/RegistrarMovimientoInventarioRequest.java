package com.agroempresa.erp.inventario.dto;

import com.agroempresa.erp.inventario.TipoMovimientoInventario;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegistrarMovimientoInventarioRequest(
        @NotNull(message = "El producto es obligatorio")
        Long productoId,

        @NotNull(message = "El tipo de movimiento es obligatorio")
        TipoMovimientoInventario tipo,

        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 1, message = "La cantidad debe ser mayor a cero")
        Integer cantidad,

        @NotBlank(message = "El motivo es obligatorio")
        @Size(max = 250, message = "El motivo no debe superar los 250 caracteres")
        String motivo
) {
}
