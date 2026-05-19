package com.agroempresa.erp.comercial.venta.devolucion.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record RegistrarDevolucionVentaRequest(
        @NotBlank(message = "El motivo de la devolucion es obligatorio")
        @Size(max = 300, message = "El motivo de la devolucion no debe superar los 300 caracteres")
        String motivo,

        @Valid
        @NotEmpty(message = "La devolucion debe tener al menos un detalle")
        @Size(max = 100, message = "La devolucion no debe superar los 100 detalles")
        List<DevolucionVentaDetalleRequest> detalles
) {
}
