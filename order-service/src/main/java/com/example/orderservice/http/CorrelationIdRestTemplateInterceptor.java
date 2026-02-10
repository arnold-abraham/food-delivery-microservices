package com.example.orderservice.http;

import com.example.contracts.CorrelationHeaders;
import org.slf4j.MDC;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

public class CorrelationIdRestTemplateInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        String correlationId = MDC.get(CorrelationHeaders.MDC_KEY);
        if (correlationId != null && !correlationId.isBlank()) {
            request.getHeaders().set(CorrelationHeaders.CORRELATION_ID, correlationId);
        }
        return execution.execute(request, body);
    }
}

