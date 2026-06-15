package com.agroempresa.erp.comercial.solicitud;

import com.agroempresa.erp.comercial.solicitud.dto.ActualizarEstadoSolicitudAtencionRequest;
import com.agroempresa.erp.comercial.solicitud.dto.SolicitudAtencionRequest;
import com.agroempresa.erp.comercial.solicitud.dto.SolicitudAtencionResponse;
import com.agroempresa.erp.common.error.BusinessException;
import com.agroempresa.erp.common.error.RecursoNoEncontradoException;
import com.agroempresa.erp.common.pagination.PaginaResponse;
import com.agroempresa.erp.common.pagination.Paginacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class SolicitudAtencionService {

    private static final Map<String, String> CAMPOS_ORDENABLES = Map.of(
            "id", "id",
            "nombre", "nombre",
            "estado", "estado",
            "creadoEn", "creadoEn",
            "actualizadoEn", "actualizadoEn"
    );

    private static final Sort ORDEN_DEFAULT = Sort.by(Sort.Direction.DESC, "creadoEn");

    private final SolicitudAtencionRepository solicitudAtencionRepository;

    public SolicitudAtencionService(SolicitudAtencionRepository solicitudAtencionRepository) {
        this.solicitudAtencionRepository = solicitudAtencionRepository;
    }

    @Transactional
    public SolicitudAtencionResponse crear(SolicitudAtencionRequest request) {
        validarRequest(request);

        SolicitudAtencion solicitud = new SolicitudAtencion(
                normalizar(request.nombre()),
                normalizar(request.documentoIdentidad()),
                normalizar(request.telefono()),
                normalizar(request.email()),
                normalizar(request.direccion()),
                normalizar(request.cultivo()),
                normalizar(request.interes()),
                normalizar(request.mensaje())
        );

        return SolicitudAtencionResponse.desdeEntidad(solicitudAtencionRepository.save(solicitud));
    }

    @Transactional(readOnly = true)
    public PaginaResponse<SolicitudAtencionResponse> listar(
            EstadoSolicitudAtencion estado,
            Integer pagina,
            Integer tamanio,
            String orden
    ) {
        var pageable = Paginacion.crear(pagina, tamanio, orden, CAMPOS_ORDENABLES, ORDEN_DEFAULT);
        Page<SolicitudAtencion> solicitudes = estado == null
                ? solicitudAtencionRepository.findAll(pageable)
                : solicitudAtencionRepository.findByEstado(estado, pageable);

        return PaginaResponse.desde(solicitudes, SolicitudAtencionResponse::desdeEntidad);
    }

    @Transactional
    public SolicitudAtencionResponse actualizarEstado(Long id, ActualizarEstadoSolicitudAtencionRequest request) {
        if (request == null || request.estado() == null) {
            throw new BusinessException("El estado de la solicitud es obligatorio");
        }

        SolicitudAtencion solicitud = solicitudAtencionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontro la solicitud con id: " + id));

        solicitud.actualizarEstado(request.estado(), normalizar(request.atendidoPor()));

        return SolicitudAtencionResponse.desdeEntidad(solicitud);
    }

    private void validarRequest(SolicitudAtencionRequest request) {
        if (request == null) {
            throw new BusinessException("Los datos de la solicitud son obligatorios");
        }

        if (request.nombre() == null || request.nombre().isBlank()) {
            throw new BusinessException("El nombre es obligatorio");
        }

        boolean tieneTelefono = request.telefono() != null && !request.telefono().isBlank();
        boolean tieneEmail = request.email() != null && !request.email().isBlank();

        if (!tieneTelefono && !tieneEmail) {
            throw new BusinessException("Indica un telefono o email para que el asesor pueda contactarte");
        }
    }

    private String normalizar(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }

        return valor.trim();
    }
}
