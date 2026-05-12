package com.agroempresa.erp.cliente.dto;

public record ClienteRequest(
        String nombre,
        String documentoIdentidad,
        String telefono,
        String email,
        String direccion
) {
}