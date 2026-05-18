package com.agroempresa.erp.proveedor.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProveedorRequest(
        @NotBlank(message = "El nombre del proveedor es obligatorio")
        @Size(max = 160, message = "El nombre no debe superar los 160 caracteres")
        String nombre,

        @Size(max = 20, message = "El documento de identidad no debe superar los 20 caracteres")
        String documentoIdentidad,

        @Size(max = 30, message = "El teléfono no debe superar los 30 caracteres")
        String telefono,

        @Email(message = "El email debe tener un formato válido")
        @Size(max = 160, message = "El email no debe superar los 160 caracteres")
        String email,

        @Size(max = 250, message = "La dirección no debe superar los 250 caracteres")
        String direccion
) {
}
