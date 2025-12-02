package com.example.paymentservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Random;

@RestController
@RequestMapping("/payments")
public class PaymentController {
    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);
    private record PaymentRequest(Long orderId, Double amount) {}
    private record PaymentResponse(Long orderId, String status) {}
    private final Random random = new Random();

    @PostMapping
    public ResponseEntity<PaymentResponse> pay(@RequestBody PaymentRequest req) {
        boolean success = random.nextDouble() > 0.1; // 90% success
        String status = success ? "SUCCESS" : "FAILED";
        log.info("Processed payment orderId={} amount={} status={}", req.orderId(), req.amount(), status);
        return ResponseEntity.ok(new PaymentResponse(req.orderId(), status));
    }
}

