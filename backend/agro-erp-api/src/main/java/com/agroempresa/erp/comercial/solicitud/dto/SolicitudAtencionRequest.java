package com.agroempresa.erp.comercial.solicitud.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SolicitudAtencionRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 160, message = "El nombre no debe superar los 160 caracteres")
        String nombre,

        @Size(max = 20, message = "El documento no debe superar los 20 caracteres")
        String documentoIdentidad,

        @Size(max = 30, message = "El telefono no debe superar los 30 caracteres")
        String telefono,

        @Email(message = "El email debe tener un formato valido")
        @Size(max = 160, message = "El email no debe superar los 160 caracteres")
        String email,

        @Size(max = 250, message = "La direccion no debe superar los 250 caracteres")
        String direccion,

        @Size(max = 120, message = "El cultivo no debe superar los 120 caracteres")
        String cultivo,

        @Size(max = 120, message = "El interes no debe superar los 120 caracteres")
        String interes,

        @Size(max = 500, message = "El mensaje no debe superar los 500 caracteres")
        String mensaje
) {
}
