package com.example.deliveryservice.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {

    List<OutboxEvent> findBySentAtIsNull();

    @Modifying
    @Query("UPDATE OutboxEvent o SET o.sentAt = :sentAt WHERE o.id = :id")
    void markSent(@Param("id") Long id, @Param("sentAt") Instant sentAt);
}
