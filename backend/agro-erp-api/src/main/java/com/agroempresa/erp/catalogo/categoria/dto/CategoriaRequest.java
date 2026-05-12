package com.agroempresa.erp.catalogo.categoria.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoriaRequest(
        @NotBlank(message = "El nombre de la categoría es obligatorio")
        @Size(max = 120, message = "El nombre no debe superar los 120 caracteres")
        String nombre,

        @Size(max = 300, message = "La descripción no debe superar los 300 caracteres")
        String descripcion
) {
}