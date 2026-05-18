package com.agroempresa.erp.seguridad.dto;

import java.time.Instant;

public record LoginResponse(
        String accessToken,
        String tokenType,
        Instant expiresAt,
        UsuarioResponse usuario
) {
}
