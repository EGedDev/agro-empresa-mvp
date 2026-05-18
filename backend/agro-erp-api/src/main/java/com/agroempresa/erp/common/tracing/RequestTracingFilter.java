package com.agroempresa.erp.common.tracing;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestTracingFilter extends OncePerRequestFilter {

    private static final int MAX_CORRELATION_ID_LENGTH = 120;
    private static final int MAX_IP_ADDRESS_LENGTH = 80;
    private static final int MAX_USER_AGENT_LENGTH = 255;
    private static final Pattern CORRELATION_ID_VALIDO = Pattern.compile("^[A-Za-z0-9._:-]{1,120}$");

    private static final String X_FORWARDED_FOR = "X-Forwarded-For";
    private static final String X_REAL_IP = "X-Real-IP";
    private static final String USER_AGENT = "User-Agent";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        RequestTrace requestTrace = new RequestTrace(
                resolverCorrelationId(request),
                resolverIpAddress(request),
                normalizar(request.getHeader(USER_AGENT), MAX_USER_AGENT_LENGTH)
        );

        RequestTraceContext.establecer(requestTrace);
        response.setHeader(RequestTraceContext.CORRELATION_ID_HEADER, requestTrace.correlationId());
        MDC.put("correlationId", requestTrace.correlationId());

        if (requestTrace.ipAddress() != null) {
            MDC.put("clientIp", requestTrace.ipAddress());
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("correlationId");
            MDC.remove("clientIp");
            RequestTraceContext.limpiar();
        }
    }

    private String resolverCorrelationId(HttpServletRequest request) {
        String correlationId = normalizar(
                request.getHeader(RequestTraceContext.CORRELATION_ID_HEADER),
                MAX_CORRELATION_ID_LENGTH
        );

        if (correlationId == null) {
            correlationId = normalizar(
                    request.getHeader(RequestTraceContext.REQUEST_ID_HEADER),
                    MAX_CORRELATION_ID_LENGTH
            );
        }

        if (correlationId == null || !CORRELATION_ID_VALIDO.matcher(correlationId).matches()) {
            return UUID.randomUUID().toString();
        }

        return correlationId;
    }

    private String resolverIpAddress(HttpServletRequest request) {
        String forwardedFor = normalizar(request.getHeader(X_FORWARDED_FOR), MAX_IP_ADDRESS_LENGTH);
        if (forwardedFor != null) {
            String primeraIp = forwardedFor.split(",", 2)[0].trim();
            return normalizar(primeraIp, MAX_IP_ADDRESS_LENGTH);
        }

        String realIp = normalizar(request.getHeader(X_REAL_IP), MAX_IP_ADDRESS_LENGTH);
        if (realIp != null) {
            return realIp;
        }

        return normalizar(request.getRemoteAddr(), MAX_IP_ADDRESS_LENGTH);
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
}
