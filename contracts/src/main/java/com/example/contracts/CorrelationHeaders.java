package com.example.contracts;

public final class CorrelationHeaders {
    private CorrelationHeaders() {}

    public static final String CORRELATION_ID = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";
}

