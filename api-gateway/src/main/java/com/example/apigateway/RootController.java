package com.example.apigateway;
}
    }
        );
                "eureka", "http://localhost:8761"
                "health", "/health",
                ),
                        "/notifications/**"
                        "/payments/**",
                        "/orders/**",
                        "/restaurants/**",
                        "/users/**",
                "routes", List.of(
                "status", "UP",
                "service", "api-gateway",
        return Map.of(
    public Map<String, Object> root() {
    @GetMapping("/")

public class RootController {
@RestController

import java.util.Map;
import java.util.List;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;


