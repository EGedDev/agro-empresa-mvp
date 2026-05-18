package com.agroempresa.erp.common.tracing;

public final class RequestTraceContext {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    private static final ThreadLocal<RequestTrace> CONTEXT = new ThreadLocal<>();

    private RequestTraceContext() {
    }

    static void establecer(RequestTrace requestTrace) {
        CONTEXT.set(requestTrace);
    }

    public static RequestTrace actual() {
        return CONTEXT.get();
    }

    public static String correlationIdActual() {
        RequestTrace requestTrace = actual();
        return requestTrace == null ? null : requestTrace.correlationId();
    }

    static void limpiar() {
        CONTEXT.remove();
    }
}
