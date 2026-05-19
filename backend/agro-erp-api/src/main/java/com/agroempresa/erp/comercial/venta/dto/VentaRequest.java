package com.agroempresa.erp.comercial.venta.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record VentaRequest(
        @NotNull(message = "El cliente es obligatorio")
        Long clienteId,

        LocalDate fechaVencimiento,

        @Valid
        @NotEmpty(message = "La venta debe tener al menos un producto")
        List<VentaDetalleRequest> detalles
) {

    public VentaRequest(Long clienteId, List<VentaDetalleRequest> detalles) {
        this(clienteId, null, detalles);
    }
}
