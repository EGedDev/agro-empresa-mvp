package com.agroempresa.erp.idempotencia;

import com.agroempresa.erp.common.tracing.RequestTraceContext;
import com.agroempresa.erp.idempotencia.IdempotencyService.DecisionIdempotencia;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.regex.Pattern;

public class IdempotencyFilter extends OncePerRequestFilter {

    public static final String REPLAY_HEADER = "Idempotency-Replayed";

    private static final Pattern VENTAS = Pattern.compile("^/api/v1/ventas/?$");
    private static final Pattern COMPRAS = Pattern.compile("^/api/v1/compras/?$");
    private static final Pattern PAGO_VENTA = Pattern.compile("^/api/v1/ventas/\\d+/pagos/?$");
    private static final Pattern PAGO_COMPRA = Pattern.compile("^/api/v1/compras/\\d+/pagos/?$");
    private static final Pattern INVENTARIO = Pattern.compile("^/api/v1/inventario/movimientos/?$");

    private final IdempotencyService idempotencyService;

    public IdempotencyFilter(IdempotencyService idempotencyService) {
        this.idempotencyService = idempotencyService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (!requiereIdempotencia(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }

        byte[] requestBody = request.getInputStream().readAllBytes();
        String username = authentication.getName();
        String ruta = request.getRequestURI();

        DecisionIdempotencia decision;
        try {
            decision = idempotencyService.iniciar(
                    username,
                    request.getMethod(),
                    ruta,
                    request.getHeader(IdempotencyService.HEADER_NAME),
                    requestBody
            );
        } catch (IdempotencyException ex) {
            escribirError(response, ex.getStatus(), ex.getCode(), ex.getMessage());
            return;
        }

        if (decision.reintento()) {
            escribirRespuestaGuardada(response, decision);
            return;
        }

        CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request, requestBody);
        ContentCachingResponseWrapper cachedResponse = new ContentCachingResponseWrapper(response);

        try {
            filterChain.doFilter(cachedRequest, cachedResponse);

            if (esRespuestaExitosa(cachedResponse.getStatus())) {
                idempotencyService.completar(
                        decision.solicitudId(),
                        cachedResponse.getStatus(),
                        cachedResponse.getContentType(),
                        cachedResponse.getContentAsByteArray()
                );
                cachedResponse.setHeader(REPLAY_HEADER, "false");
            } else {
                idempotencyService.descartar(decision.solicitudId());
            }
        } catch (RuntimeException | ServletException | IOException ex) {
            idempotencyService.descartar(decision.solicitudId());
            throw ex;
        } finally {
            cachedResponse.copyBodyToResponse();
        }
    }

    private boolean requiereIdempotencia(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return false;
        }

        String ruta = request.getRequestURI();
        return VENTAS.matcher(ruta).matches()
                || COMPRAS.matcher(ruta).matches()
                || PAGO_VENTA.matcher(ruta).matches()
                || PAGO_COMPRA.matcher(ruta).matches()
                || INVENTARIO.matcher(ruta).matches();
    }

    private boolean esRespuestaExitosa(int status) {
        return status >= 200 && status < 300;
    }

    private void escribirRespuestaGuardada(
            HttpServletResponse response,
            DecisionIdempotencia decision
    ) throws IOException {
        response.setStatus(decision.responseStatus());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader(REPLAY_HEADER, "true");

        if (decision.responseContentType() != null) {
            response.setContentType(decision.responseContentType());
        }

        String body = decision.responseBody();
        if (body != null && !body.isBlank()) {
            response.getWriter().write(body);
        }
    }

    private void escribirError(
            HttpServletResponse response,
            HttpStatus status,
            String codigo,
            String mensaje
    ) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("""
                {"code":"%s","message":"%s","correlationId":%s,"timestamp":"%s"}\
                """.formatted(
                codigo,
                escaparJson(mensaje),
                jsonStringOrNull(RequestTraceContext.correlationIdActual()),
                Instant.now()
        ));
    }

    private String escaparJson(String valor) {
        return valor.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String jsonStringOrNull(String valor) {
        return valor == null ? "null" : "\"" + escaparJson(valor) + "\"";
    }
}
