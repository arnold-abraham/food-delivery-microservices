package com.example.deliveryservice.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(prefix = "app.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final long sendTimeoutMs;

    public OutboxRelay(
            OutboxRepository outboxRepository,
            @Qualifier("outboxKafkaTemplate") KafkaTemplate<String, String> kafkaTemplate,
            @Value("${app.kafka.send-timeout-ms:200}") long sendTimeoutMs
    ) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.sendTimeoutMs = sendTimeoutMs;
    }

    @Scheduled(fixedDelayString = "${app.outbox.relay-interval-ms:1000}")
    @Transactional
    public void relay() {
        List<OutboxEvent> pending = outboxRepository.findBySentAtIsNull();
        for (OutboxEvent event : pending) {
            try {
                kafkaTemplate.send(event.getTopic(), event.getEventKey(), event.getPayload())
                        .get(sendTimeoutMs, TimeUnit.MILLISECONDS);
                outboxRepository.markSent(event.getId(), Instant.now());
                log.info("Outbox relayed id={} topic={} key={}", event.getId(), event.getTopic(), event.getEventKey());
            } catch (Exception ex) {
                log.warn("Outbox relay failed id={} topic={} key={}: {}",
                        event.getId(), event.getTopic(), event.getEventKey(), ex.getMessage());
            }
        }
    }
}
