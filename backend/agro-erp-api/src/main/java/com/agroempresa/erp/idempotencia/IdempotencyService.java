package com.agroempresa.erp.idempotencia;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
public class IdempotencyService {

    public static final String HEADER_NAME = "Idempotency-Key";
    public static final int MAX_KEY_LENGTH = 120;

    private final SolicitudIdempotenteRepository solicitudIdempotenteRepository;

    public IdempotencyService(SolicitudIdempotenteRepository solicitudIdempotenteRepository) {
        this.solicitudIdempotenteRepository = solicitudIdempotenteRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public DecisionIdempotencia iniciar(
            String username,
            String metodoHttp,
            String ruta,
            String idempotencyKey,
            byte[] requestBody
    ) {
        String keyNormalizada = validarYNormalizarKey(idempotencyKey);
        String requestHash = calcularHash(requestBody);

        return solicitudIdempotenteRepository.findByScopeParaActualizar(
                        username,
                        metodoHttp,
                        ruta,
                        keyNormalizada
                )
                .map(solicitud -> resolverSolicitudExistente(solicitud, requestHash))
                .orElseGet(() -> registrarSolicitudNueva(
                        keyNormalizada,
                        username,
                        metodoHttp,
                        ruta,
                        requestHash
                ));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void completar(
            Long solicitudId,
            int responseStatus,
            String responseContentType,
            byte[] responseBody
    ) {
        SolicitudIdempotente solicitud = solicitudIdempotenteRepository.findById(solicitudId)
                .orElseThrow(() -> new IllegalStateException("No se encontro la solicitud idempotente"));

        solicitud.completar(
                responseStatus,
                responseContentType,
                responseBody == null ? null : new String(responseBody, StandardCharsets.UTF_8)
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void descartar(Long solicitudId) {
        solicitudIdempotenteRepository.deleteById(solicitudId);
    }

    public String validarYNormalizarKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IdempotencyException(
                    HttpStatus.PRECONDITION_REQUIRED,
                    "IDEMPOTENCY_KEY_REQUIRED",
                    "El header Idempotency-Key es obligatorio para esta operacion"
            );
        }

        String keyNormalizada = idempotencyKey.trim();
        if (keyNormalizada.length() > MAX_KEY_LENGTH) {
            throw new IdempotencyException(
                    HttpStatus.BAD_REQUEST,
                    "IDEMPOTENCY_KEY_INVALID",
                    "El header Idempotency-Key no debe superar los " + MAX_KEY_LENGTH + " caracteres"
            );
        }

        return keyNormalizada;
    }

    private DecisionIdempotencia resolverSolicitudExistente(
            SolicitudIdempotente solicitud,
            String requestHash
    ) {
        if (!solicitud.getRequestHash().equals(requestHash)) {
            throw new IdempotencyException(
                    HttpStatus.CONFLICT,
                    "IDEMPOTENCY_KEY_CONFLICT",
                    "La clave de idempotencia ya fue usada con una solicitud diferente"
            );
        }

        if (solicitud.getEstado() == EstadoSolicitudIdempotente.COMPLETADA) {
            return DecisionIdempotencia.reintento(
                    solicitud.getResponseStatus(),
                    solicitud.getResponseContentType(),
                    solicitud.getResponseBody()
            );
        }

        throw new IdempotencyException(
                HttpStatus.CONFLICT,
                "IDEMPOTENCY_REQUEST_IN_PROGRESS",
                "La solicitud con esta clave de idempotencia aun esta en proceso"
        );
    }

    private DecisionIdempotencia registrarSolicitudNueva(
            String idempotencyKey,
            String username,
            String metodoHttp,
            String ruta,
            String requestHash
    ) {
        try {
            SolicitudIdempotente solicitud = new SolicitudIdempotente(
                    idempotencyKey,
                    username,
                    metodoHttp,
                    ruta,
                    requestHash
            );

            SolicitudIdempotente solicitudGuardada = solicitudIdempotenteRepository.saveAndFlush(solicitud);
            return DecisionIdempotencia.nueva(solicitudGuardada.getId());
        } catch (DataIntegrityViolationException ex) {
            throw new IdempotencyException(
                    HttpStatus.CONFLICT,
                    "IDEMPOTENCY_REQUEST_IN_PROGRESS",
                    "La solicitud con esta clave de idempotencia aun esta en proceso"
            );
        }
    }

    private String calcularHash(byte[] requestBody) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(requestBody == null ? new byte[0] : requestBody));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no esta disponible", ex);
        }
    }

    public record DecisionIdempotencia(
            Long solicitudId,
            boolean reintento,
            Integer responseStatus,
            String responseContentType,
            String responseBody
    ) {

        public static DecisionIdempotencia nueva(Long solicitudId) {
            return new DecisionIdempotencia(solicitudId, false, null, null, null);
        }

        public static DecisionIdempotencia reintento(
                Integer responseStatus,
                String responseContentType,
                String responseBody
        ) {
            return new DecisionIdempotencia(null, true, responseStatus, responseContentType, responseBody);
        }
    }
}
