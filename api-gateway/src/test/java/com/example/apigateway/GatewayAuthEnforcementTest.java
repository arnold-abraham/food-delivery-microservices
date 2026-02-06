package com.example.apigateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Minimal security regression test:
 * - /health is public
 * - other routes require authentication (JWT)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@org.springframework.test.context.ActiveProfiles("test") // Ensure Eureka/discovery are disabled for unit tests
class GatewayAuthEnforcementTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void health_isPublic() {
        webTestClient.get()
                .uri("/health")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void health_includesCorrelationIdHeader() {
        webTestClient.get()
                .uri("/health")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().exists("X-Correlation-Id");
    }

    @Test
    void health_echoesProvidedCorrelationIdHeader() {
        webTestClient.get()
                .uri("/health")
                .header("X-Correlation-Id", "test-corr-123")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("X-Correlation-Id", "test-corr-123");
    }

    @Test
    void root_isPublic() {
        webTestClient.get()
                .uri("/")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void usersEndpoint_requiresAuth() {
        // We don't need downstream services running here.
        // If gateway security works, we should get 401 before routing.
        webTestClient.get()
                .uri("/users")
                .exchange()
                .expectStatus().isUnauthorized();
    }
}
