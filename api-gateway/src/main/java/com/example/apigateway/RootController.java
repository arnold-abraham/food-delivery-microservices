package com.example.apigateway;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class RootController {

    @GetMapping("/")
    public Map<String, Object> root() {
        return Map.of(
                "service", "api-gateway",
                "status", "UP",
                "routes", List.of(
                        "/users/**",
                        "/restaurants/**",
                        "/orders/**",
                        "/payments/**",
                        "/notifications/**"
                ),
                "health", "/health",
                "eureka", "http://localhost:8761"
        );
    }
}
