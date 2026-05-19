package com.agroempresa.erp.comercial.compra.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record CompraRequest(
        @NotNull(message = "El proveedor es obligatorio")
        Long proveedorId,

        LocalDate fechaVencimiento,

        @Valid
        @NotEmpty(message = "La compra debe tener al menos un producto")
        List<CompraDetalleRequest> detalles
) {

    public CompraRequest(Long proveedorId, List<CompraDetalleRequest> detalles) {
        this(proveedorId, null, detalles);
    }
}
