package com.agroempresa.erp.auditoria;

import com.agroempresa.erp.auditoria.dto.AuditoriaEventoResponse;
import com.agroempresa.erp.common.error.BusinessException;
import com.agroempresa.erp.common.pagination.PaginaResponse;
import com.agroempresa.erp.common.pagination.Paginacion;
import com.agroempresa.erp.common.tracing.RequestTrace;
import com.agroempresa.erp.common.tracing.RequestTraceContext;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Service
public class AuditoriaService {

    private static final String USUARIO_SISTEMA = "sistema";
    private static final int MAX_DETALLE_LENGTH = 500;
    private static final int MAX_CORRELATION_ID_LENGTH = 120;
    private static final int MAX_IP_ADDRESS_LENGTH = 80;
    private static final int MAX_USER_AGENT_LENGTH = 255;

    private static final Map<String, String> CAMPOS_ORDENABLES = Map.of(
            "id", "id",
            "username", "username",
            "accion", "accion",
            "recursoTipo", "recursoTipo",
            "recursoId", "recursoId",
            "correlationId", "correlationId",
            "creadoEn", "creadoEn"
    );

    private static final Sort ORDEN_DEFAULT = Sort.by(Sort.Direction.DESC, "creadoEn");

    private final AuditoriaEventoRepository auditoriaEventoRepository;

    public AuditoriaService(AuditoriaEventoRepository auditoriaEventoRepository) {
        this.auditoriaEventoRepository = auditoriaEventoRepository;
    }

    @Transactional(readOnly = true)
    public PaginaResponse<AuditoriaEventoResponse> listarEventos(
            String username,
            String accion,
            String recursoTipo,
            Long recursoId,
            String correlationId,
            LocalDate desde,
            LocalDate hasta,
            Integer pagina,
            Integer tamanio,
            String orden
    ) {
        validarRangoFechas(desde, hasta);

        return PaginaResponse.desde(
                auditoriaEventoRepository.buscar(
                        Paginacion.normalizarTexto(username),
                        Paginacion.normalizarTexto(accion),
                        Paginacion.normalizarTexto(recursoTipo),
                        recursoId,
                        Paginacion.normalizarTexto(correlationId),
                        inicioDia(desde),
                        inicioDiaPosterior(hasta),
                        Paginacion.crear(pagina, tamanio, orden, CAMPOS_ORDENABLES, ORDEN_DEFAULT)
                ),
                AuditoriaEventoResponse::desdeEntidad
        );
    }

    @Transactional
    public void registrar(String accion, String recursoTipo, Long recursoId, String detalle) {
        RequestTrace requestTrace = RequestTraceContext.actual();
        AuditoriaEvento evento = new AuditoriaEvento(
                obtenerUsernameActual(),
                accion,
                recursoTipo,
                recursoId,
                normalizar(detalle, MAX_DETALLE_LENGTH),
                normalizar(requestTrace == null ? null : requestTrace.correlationId(), MAX_CORRELATION_ID_LENGTH),
                normalizar(requestTrace == null ? null : requestTrace.ipAddress(), MAX_IP_ADDRESS_LENGTH),
                normalizar(requestTrace == null ? null : requestTrace.userAgent(), MAX_USER_AGENT_LENGTH)
        );

        auditoriaEventoRepository.save(evento);
    }

    private String obtenerUsernameActual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return USUARIO_SISTEMA;
        }

        return authentication.getName();
    }

    private String normalizar(String valor, int longitudMaxima) {
        if (valor == null || valor.isBlank()) {
            return null;
        }

        String valorNormalizado = valor.trim();
        return valorNormalizado.length() <= longitudMaxima
                ? valorNormalizado
                : valorNormalizado.substring(0, longitudMaxima);
    }

    private void validarRangoFechas(LocalDate desde, LocalDate hasta) {
        if (desde != null && hasta != null && hasta.isBefore(desde)) {
            throw new BusinessException("La fecha final no puede ser anterior a la fecha inicial");
        }
    }

    private LocalDateTime inicioDia(LocalDate fecha) {
        return fecha == null ? null : fecha.atStartOfDay();
    }

    private LocalDateTime inicioDiaPosterior(LocalDate fecha) {
        return fecha == null ? null : fecha.plusDays(1).atStartOfDay();
    }
}
