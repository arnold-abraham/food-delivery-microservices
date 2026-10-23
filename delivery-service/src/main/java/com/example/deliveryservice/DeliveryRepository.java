package com.example.deliveryservice;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {
    Optional<Delivery> findFirstByOrderId(Long orderId);
    List<Delivery> findByDriverId(Long driverId);
}
