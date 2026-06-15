package com.agroempresa.erp.comercial.solicitud.dto;

import com.agroempresa.erp.comercial.solicitud.EstadoSolicitudAtencion;
import com.agroempresa.erp.comercial.solicitud.SolicitudAtencion;

import java.time.LocalDateTime;

public record SolicitudAtencionResponse(
        Long id,
        String nombre,
        String documentoIdentidad,
        String telefono,
        String email,
        String direccion,
        String cultivo,
        String interes,
        String mensaje,
        EstadoSolicitudAtencion estado,
        String atendidoPor,
        LocalDateTime creadoEn,
        LocalDateTime actualizadoEn
) {

    public static SolicitudAtencionResponse desdeEntidad(SolicitudAtencion solicitud) {
        return new SolicitudAtencionResponse(
                solicitud.getId(),
                solicitud.getNombre(),
                solicitud.getDocumentoIdentidad(),
                solicitud.getTelefono(),
                solicitud.getEmail(),
                solicitud.getDireccion(),
                solicitud.getCultivo(),
                solicitud.getInteres(),
                solicitud.getMensaje(),
                solicitud.getEstado(),
                solicitud.getAtendidoPor(),
                solicitud.getCreadoEn(),
                solicitud.getActualizadoEn()
        );
    }
}
