package com.agroempresa.erp.proveedor.dto;

import com.agroempresa.erp.proveedor.Proveedor;

import java.time.LocalDateTime;

public record ProveedorResponse(
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

    public static ProveedorResponse desdeEntidad(Proveedor proveedor) {
        return new ProveedorResponse(
                proveedor.getId(),
                proveedor.getNombre(),
                proveedor.getDocumentoIdentidad(),
                proveedor.getTelefono(),
                proveedor.getEmail(),
                proveedor.getDireccion(),
                proveedor.getActivo(),
                proveedor.getCreadoEn(),
                proveedor.getActualizadoEn()
        );
    }
}
