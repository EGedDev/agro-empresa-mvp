package com.agroempresa.erp.comercial.compra.devolucion.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record DevolucionCompraDetalleRequest(
        @NotNull(message = "El detalle de compra es obligatorio")
        @Positive(message = "El id del detalle de compra debe ser mayor a cero")
        Long compraDetalleId,

        @NotNull(message = "La cantidad es obligatoria")
        @Positive(message = "La cantidad debe ser mayor a cero")
        Integer cantidad
) {
}
