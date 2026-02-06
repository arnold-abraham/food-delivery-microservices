package com.example.contracts.events;

import com.example.contracts.events.OrderEventsV1.OrderCreatedV1;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertTrue;

class EventSerializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void orderCreatedV1_serializesRequiredFields() throws Exception {
        OrderCreatedV1 event = OrderCreatedV1.v1(
                10L,
                20L,
                30L,
                new BigDecimal("22.00"),
                Instant.parse("2026-02-06T00:00:00Z"),
                "corr-123"
        );

        String json = objectMapper.writeValueAsString(event);
        assertTrue(json.contains("\"eventVersion\":1"));
        assertTrue(json.contains("\"orderId\":10"));
        assertTrue(json.contains("\"correlationId\":\"corr-123\""));
    }
}
