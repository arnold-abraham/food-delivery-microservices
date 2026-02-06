package com.example.apigateway;

import com.example.contracts.CorrelationHeaders;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Adds/propagates X-Correlation-Id for requests served directly by the gateway itself
 * (e.g., /health, /) where Spring Cloud Gateway's GlobalFilters aren't guaranteed to run.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdWebFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String incoming = exchange.getRequest().getHeaders().getFirst(CorrelationHeaders.CORRELATION_ID);
        String correlationId = (incoming == null || incoming.isBlank()) ? UUID.randomUUID().toString() : incoming;

        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header(CorrelationHeaders.CORRELATION_ID, correlationId)
                .build();

        ServerWebExchange mutatedExchange = exchange.mutate().request(mutatedRequest).build();

        // Must set before commit; headers can become read-only after response is committed.
        mutatedExchange.getResponse().beforeCommit(() -> {
            mutatedExchange.getResponse().getHeaders().set(CorrelationHeaders.CORRELATION_ID, correlationId);
            return Mono.empty();
        });

        return chain.filter(mutatedExchange);
    }
}
