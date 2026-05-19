package com.agroempresa.erp.comercial.venta.devolucion.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record DevolucionVentaDetalleRequest(
        @NotNull(message = "El detalle de venta es obligatorio")
        @Positive(message = "El id del detalle de venta debe ser mayor a cero")
        Long ventaDetalleId,

        @NotNull(message = "La cantidad es obligatoria")
        @Positive(message = "La cantidad debe ser mayor a cero")
        Integer cantidad
) {
}
