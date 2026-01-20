package com.example.orderservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class OrderFlowSmokeTest {

    @Autowired
    TestRestTemplate http;

    @Autowired
    MockRestServiceServer mockServer;

    @TestConfiguration
    static class Config {
        @Bean
        @Primary
        RestTemplate restTemplate(RestTemplateBuilder builder) {
            return builder.build();
        }

        @Bean
        MockRestServiceServer mockRestServiceServer(RestTemplate restTemplate) {
            return MockRestServiceServer.createServer(restTemplate);
        }
    }

    @Test
    void createOrderWithItems_payAndAssignDelivery() {
        // restaurant-service: menu item lookup
        mockServer.expect(requestTo("http://localhost/restaurants/1/menu/10"))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withSuccess("{\"id\":10,\"restaurantId\":1,\"name\":\"Pizza\",\"price\":12}", MediaType.APPLICATION_JSON));

        // payment-service: exact amount
        mockServer.expect(requestTo("http://localhost/payments"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withSuccess("{\"orderId\":1,\"status\":\"SUCCESS\",\"message\":\"Payment accepted\"}", MediaType.APPLICATION_JSON));

        // user-service: driver exists
        mockServer.expect(requestTo("http://localhost/users/5"))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withSuccess("{\"id\":5,\"name\":\"Driver\"}", MediaType.APPLICATION_JSON));

        // delivery-service: create assignment
        mockServer.expect(requestTo("http://localhost/deliveries"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withSuccess("{\"id\":99,\"orderId\":1,\"driverId\":5,\"status\":\"ASSIGNED\"}", MediaType.APPLICATION_JSON));

        // Create order with items
        Map<String, Object> createReq = Map.of(
                "userId", 2,
                "restaurantId", 1,
                "items", new Object[]{Map.of("menuItemId", 10, "quantity", 2)}
        );

        var created = http.postForEntity("/orders", createReq, Map.class);
        assertThat(created.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(created.getBody()).isNotNull();
        assertThat(((Number) created.getBody().get("totalAmount")).doubleValue()).isEqualTo(24.0);

        Number orderId = (Number) created.getBody().get("id");

        // Pay order
        Map<String, Object> payReq = Map.of("amount", 24, "driverId", 5);
        var paid = http.postForEntity("/orders/" + orderId + "/pay", payReq, Map.class);
        assertThat(paid.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(paid.getBody()).isNotNull();
        assertThat(String.valueOf(paid.getBody().get("status"))).isEqualTo("PAID");
        assertThat(String.valueOf(paid.getBody().get("deliveryStatus"))).isEqualTo("ASSIGNED");

        mockServer.verify();
    }
}
