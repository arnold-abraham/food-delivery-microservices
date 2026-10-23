package com.example.apigateway.security;

import org.springframework.core.Ordered;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Extracts userId and roles from the validated JWT and forwards them as
 * X-User-Id / X-User-Roles headers so downstream services can make
 * authorization decisions without re-validating the token themselves.
 */
@Component
public class JwtClaimsRelayFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return exchange.getPrincipal()
                .ofType(JwtAuthenticationToken.class)
                .map(auth -> {
                    String userId = auth.getToken().getSubject();
                    List<String> roles = auth.getToken().getClaimAsStringList("roles");
                    String rolesHeader = roles != null ? String.join(",", roles) : "";

                    return exchange.mutate()
                            .request(r -> r
                                    .header("X-User-Id", userId)
                                    .header("X-User-Roles", rolesHeader))
                            .build();
                })
                .defaultIfEmpty(exchange)
                .flatMap(chain::filter);
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 1;
    }
}
