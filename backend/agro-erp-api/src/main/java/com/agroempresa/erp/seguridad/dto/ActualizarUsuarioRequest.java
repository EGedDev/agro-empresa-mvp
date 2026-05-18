package com.agroempresa.erp.seguridad.dto;

import com.agroempresa.erp.seguridad.RolUsuario;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ActualizarUsuarioRequest(
        @NotBlank(message = "El nombre del usuario es obligatorio")
        @Size(max = 160, message = "El nombre no debe superar los 160 caracteres")
        String nombre,

        @NotNull(message = "El rol es obligatorio")
        RolUsuario rol
) {
}
