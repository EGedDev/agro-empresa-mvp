package com.agroempresa.erp.seguridad.dto;

import com.agroempresa.erp.seguridad.RolUsuario;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CrearUsuarioRequest(
        @NotBlank(message = "El usuario es obligatorio")
        @Size(max = 80, message = "El usuario no debe superar los 80 caracteres")
        String username,

        @NotBlank(message = "La contrasena es obligatoria")
        @Size(min = 8, max = 120, message = "La contrasena debe tener entre 8 y 120 caracteres")
        String password,

        @NotBlank(message = "El nombre del usuario es obligatorio")
        @Size(max = 160, message = "El nombre no debe superar los 160 caracteres")
        String nombre,

        @NotNull(message = "El rol es obligatorio")
        RolUsuario rol
) {
}
