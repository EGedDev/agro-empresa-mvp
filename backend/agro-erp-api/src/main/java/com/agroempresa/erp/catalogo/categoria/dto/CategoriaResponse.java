package com.agroempresa.erp.catalogo.categoria.dto;

import java.time.LocalDateTime;

public record CategoriaResponse(
        Long id,
        String nombre,
        String descripcion,
        Boolean activo,
        LocalDateTime creadoEn,
        LocalDateTime actualizadoEn
) {
}