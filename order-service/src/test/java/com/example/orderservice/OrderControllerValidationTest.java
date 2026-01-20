package com.example.orderservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
class OrderControllerValidationTest {

    @TestConfiguration
    static class StubConfig {
        @Bean
        OrderService orderService() {
            // Not used in these tests (validation fails before controller calls service)
            return new OrderService(null, null) {
            };
        }
    }

    @Autowired
    private MockMvc mvc;

    @Test
    void createOrder_invalidPayload_returns400WithFields() throws Exception {
        mvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":0,\"restaurantId\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.fields.userId").exists())
                .andExpect(jsonPath("$.fields.restaurantId").exists());
    }

    @Test
    void pay_invalidPayload_returns400WithFields() throws Exception {
        mvc.perform(post("/orders/1/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.fields.amount").exists());
    }

    @Test
    void pay_invalidDriverId_returns400WithFields() throws Exception {
        mvc.perform(post("/orders/1/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":10,\"driverId\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.fields.driverId").exists());
    }
}
