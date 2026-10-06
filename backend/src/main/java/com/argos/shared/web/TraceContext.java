package com.argos.shared.web;

import org.slf4j.MDC;

/** Identificador de rastreio da requisição atual (traceId do OpenTelemetry quando existir; senão o correlationId). */
public final class TraceContext {

    public static final String CORRELATION_ID = "correlationId";
    public static final String TRACE_ID = "traceId";
    public static final String USER_ID = "userId";

    private TraceContext() {
    }

    public static String currentId() {
        String traceId = MDC.get(TRACE_ID);
        return traceId != null ? traceId : MDC.get(CORRELATION_ID);
    }
}
