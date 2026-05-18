package com.agroempresa.erp.seguridad.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "El usuario es obligatorio")
        @Size(max = 80, message = "El usuario no debe superar los 80 caracteres")
        String username,

        @NotBlank(message = "La contrasena es obligatoria")
        @Size(max = 120, message = "La contrasena no debe superar los 120 caracteres")
        String password
) {
}
