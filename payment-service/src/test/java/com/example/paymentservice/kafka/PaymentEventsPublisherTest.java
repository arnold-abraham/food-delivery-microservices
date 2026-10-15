package com.example.paymentservice.kafka;

import com.example.contracts.events.PaymentCompletedEvent;
import com.example.contracts.events.PaymentRequestedEvent;
import com.example.contracts.topics.PaymentKafkaTopics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class PaymentEventsPublisherTest {

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
    private PaymentEventsPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new PaymentEventsPublisher(kafkaTemplate, 200);
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));
    }

    @Test
    void publishPaymentRequested_sendsToCorrectTopic() {
        publisher.publishPaymentRequested(1L, BigDecimal.valueOf(50), BigDecimal.valueOf(50));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(eq(PaymentKafkaTopics.PAYMENT_REQUESTED), eq("1"), eventCaptor.capture());

        PaymentRequestedEvent event = (PaymentRequestedEvent) eventCaptor.getValue();
        assertThat(event.orderId()).isEqualTo(1L);
        assertThat(event.amount()).isEqualByComparingTo(BigDecimal.valueOf(50));
        assertThat(event.expectedAmount()).isEqualByComparingTo(BigDecimal.valueOf(50));
        assertThat(event.eventVersion()).isEqualTo(PaymentRequestedEvent.VERSION);
        assertThat(event.createdAt()).isNotNull();
    }

    @Test
    void publishPaymentCompleted_sendsSuccessToCorrectTopic() {
        publisher.publishPaymentCompleted(2L, BigDecimal.valueOf(100), BigDecimal.valueOf(100), "SUCCESS");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(eq(PaymentKafkaTopics.PAYMENT_COMPLETED), eq("2"), eventCaptor.capture());

        PaymentCompletedEvent event = (PaymentCompletedEvent) eventCaptor.getValue();
        assertThat(event.orderId()).isEqualTo(2L);
        assertThat(event.status()).isEqualTo("SUCCESS");
        assertThat(event.amount()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(event.eventVersion()).isEqualTo(PaymentCompletedEvent.VERSION);
    }

    @Test
    void publishPaymentCompleted_sendsFailedStatus() {
        publisher.publishPaymentCompleted(3L, BigDecimal.valueOf(90), BigDecimal.valueOf(100), "FAILED");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(eq(PaymentKafkaTopics.PAYMENT_COMPLETED), eq("3"), eventCaptor.capture());

        assertThat(((PaymentCompletedEvent) eventCaptor.getValue()).status()).isEqualTo("FAILED");
    }

    @Test
    void publishPaymentRequested_swallowsKafkaFailure() {
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenThrow(new RuntimeException("Kafka unavailable"));

        assertThatNoException().isThrownBy(() ->
                publisher.publishPaymentRequested(4L, BigDecimal.TEN, BigDecimal.TEN));
    }

    @Test
    void publishPaymentCompleted_swallowsKafkaFailure() {
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenThrow(new RuntimeException("Kafka unavailable"));

        assertThatNoException().isThrownBy(() ->
                publisher.publishPaymentCompleted(5L, BigDecimal.TEN, BigDecimal.TEN, "SUCCESS"));
    }
}
