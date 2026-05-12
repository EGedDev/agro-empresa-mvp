package com.agroempresa.erp.cliente.dto;

import com.agroempresa.erp.cliente.Cliente;

import java.time.LocalDateTime;

public record ClienteResponse(
        Long id,
        String nombre,
        String documentoIdentidad,
        String telefono,
        String email,
        String direccion,
        Boolean activo,
        LocalDateTime creadoEn,
        LocalDateTime actualizadoEn
) {

    public static ClienteResponse desdeEntidad(Cliente cliente) {
        return new ClienteResponse(
                cliente.getId(),
                cliente.getNombre(),
                cliente.getDocumentoIdentidad(),
                cliente.getTelefono(),
                cliente.getEmail(),
                cliente.getDireccion(),
                cliente.getActivo(),
                cliente.getCreadoEn(),
                cliente.getActualizadoEn()
        );
    }
}