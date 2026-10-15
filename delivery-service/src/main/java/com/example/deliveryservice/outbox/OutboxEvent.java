package com.example.deliveryservice.outbox;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "outbox_events")
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String topic;

    @Column(name = "event_key", nullable = false)
    private String eventKey;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column
    private Instant sentAt;

    protected OutboxEvent() {}

    public OutboxEvent(String topic, String eventKey, String payload) {
        this.topic = topic;
        this.eventKey = eventKey;
        this.payload = payload;
        this.createdAt = Instant.now();
    }

    public Long getId()          { return id; }
    public String getTopic()     { return topic; }
    public String getEventKey()  { return eventKey; }
    public String getPayload()   { return payload; }
    public Instant getCreatedAt(){ return createdAt; }
    public Instant getSentAt()   { return sentAt; }
}
