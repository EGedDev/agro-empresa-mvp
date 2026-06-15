package com.agroempresa.erp.comercial.solicitud.dto;

import com.agroempresa.erp.comercial.solicitud.EstadoSolicitudAtencion;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ActualizarEstadoSolicitudAtencionRequest(
        @NotNull(message = "El estado es obligatorio")
        EstadoSolicitudAtencion estado,

        @Size(max = 120, message = "El responsable no debe superar los 120 caracteres")
        String atendidoPor
) {
}
