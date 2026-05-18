package com.agroempresa.erp.seguridad.dto;

import com.agroempresa.erp.seguridad.RolUsuario;
import com.agroempresa.erp.seguridad.Usuario;

public record UsuarioResponse(
        Long id,
        String username,
        String nombre,
        RolUsuario rol,
        Boolean activo
) {

    public static UsuarioResponse desdeEntidad(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getUsername(),
                usuario.getNombre(),
                usuario.getRol(),
                usuario.getActivo()
        );
    }
}
