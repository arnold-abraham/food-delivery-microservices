package com.example.deliveryservice;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {
    Optional<Delivery> findFirstByOrderId(Long orderId);
    Page<Delivery> findByDriverId(Long driverId, Pageable pageable);
}
