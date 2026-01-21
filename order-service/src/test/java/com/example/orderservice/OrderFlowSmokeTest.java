package com.example.orderservice;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderFlowSmokeTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    RestTemplate restTemplate;

    MockRestServiceServer mockServer;

    @BeforeEach
    void setup() {
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    void createOrderWithItems_payAndAssignDelivery() throws Exception {
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
        String createJson = "{\"userId\":2,\"restaurantId\":1,\"items\":[{\"menuItemId\":10,\"quantity\":2}]}";

        String createdBody = mvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").value(24.0))
                .andReturn().getResponse().getContentAsString();

        // Extract orderId without adding extra deps
        long orderId = Long.parseLong(createdBody.replaceAll("(?s).*\"id\"\\s*:\\s*(\\d+).*", "$1"));
        assertThat(orderId).isGreaterThan(0);

        // Pay order
        String payJson = "{\"amount\":24,\"driverId\":5}";
        mvc.perform(post("/orders/" + orderId + "/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.deliveryStatus").value("ASSIGNED"));

        mockServer.verify();
    }
}
