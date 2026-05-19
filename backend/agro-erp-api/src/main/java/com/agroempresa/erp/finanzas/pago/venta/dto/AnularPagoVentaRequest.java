package com.agroempresa.erp.finanzas.pago.venta.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnularPagoVentaRequest(
        @NotBlank(message = "El motivo de anulacion es obligatorio")
        @Size(max = 300, message = "El motivo de anulacion no debe superar los 300 caracteres")
        String motivo
) {
}
