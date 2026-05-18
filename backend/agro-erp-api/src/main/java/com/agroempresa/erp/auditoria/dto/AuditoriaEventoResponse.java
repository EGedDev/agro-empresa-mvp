package com.agroempresa.erp.auditoria.dto;

import com.agroempresa.erp.auditoria.AuditoriaEvento;

import java.time.LocalDateTime;

public record AuditoriaEventoResponse(
        Long id,
        String username,
        String accion,
        String recursoTipo,
        Long recursoId,
        String detalle,
        String correlationId,
        String ipAddress,
        String userAgent,
        LocalDateTime creadoEn
) {

    public static AuditoriaEventoResponse desdeEntidad(AuditoriaEvento evento) {
        return new AuditoriaEventoResponse(
                evento.getId(),
                evento.getUsername(),
                evento.getAccion(),
                evento.getRecursoTipo(),
                evento.getRecursoId(),
                evento.getDetalle(),
                evento.getCorrelationId(),
                evento.getIpAddress(),
                evento.getUserAgent(),
                evento.getCreadoEn()
        );
    }
}
