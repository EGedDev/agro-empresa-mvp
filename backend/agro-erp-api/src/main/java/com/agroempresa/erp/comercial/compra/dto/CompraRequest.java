package com.agroempresa.erp.comercial.compra.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CompraRequest(
        @NotNull(message = "El proveedor es obligatorio")
        Long proveedorId,

        @Valid
        @NotEmpty(message = "La compra debe tener al menos un producto")
        List<CompraDetalleRequest> detalles
) {
}
