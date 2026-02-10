package com.example.apigateway;

import com.example.contracts.CorrelationHeaders;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class CorrelationIdFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String incoming = exchange.getRequest().getHeaders().getFirst(CorrelationHeaders.CORRELATION_ID);
        String correlationId = (incoming == null || incoming.isBlank()) ? UUID.randomUUID().toString() : incoming;

        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header(CorrelationHeaders.CORRELATION_ID, correlationId)
                .build();

        ServerWebExchange mutatedExchange = exchange.mutate().request(mutatedRequest).build();

        return chain.filter(mutatedExchange)
                .doFinally(signalType -> mutatedExchange.getResponse().getHeaders().set(CorrelationHeaders.CORRELATION_ID, correlationId));
    }

    @Override
    public int getOrder() {
        return -2; // run before RequestLoggingFilter
    }
}
