package com.example.deliveryservice.kafka;

import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DefaultErrorHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class KafkaErrorHandlerConfigTest {

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
    private final KafkaErrorHandlerConfig config = new KafkaErrorHandlerConfig();

    @Test
    void kafkaErrorHandler_returnsDefaultErrorHandler() {
        DefaultErrorHandler handler = config.kafkaErrorHandler(kafkaTemplate);
        assertThat(handler).isNotNull().isInstanceOf(DefaultErrorHandler.class);
    }

    @Test
    void kafkaErrorHandler_isNewInstancePerCall() {
        DefaultErrorHandler h1 = config.kafkaErrorHandler(kafkaTemplate);
        DefaultErrorHandler h2 = config.kafkaErrorHandler(kafkaTemplate);
        assertThat(h1).isNotSameAs(h2);
    }
}
