package com.example.deliveryservice.http;

import com.example.contracts.CorrelationHeaders;
import org.slf4j.MDC;

/**
 * Minimal helper to read the current correlationId from MDC.
 */
public final class CorrelationIdHolder {
    private CorrelationIdHolder() {}

    public static String currentOrNull() {
        return MDC.get(CorrelationHeaders.MDC_KEY);
    }
}

