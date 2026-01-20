package com.example.paymentservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments")
public class PaymentController {
    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private record PaymentRequest(Long orderId, Double amount, Double expectedAmount) {}
    private record PaymentResponse(Long orderId, String status, String message) {}

    @PostMapping
    public ResponseEntity<PaymentResponse> pay(@RequestBody PaymentRequest req) {
        boolean ok = req != null
                && req.orderId() != null
                && req.amount() != null
                && req.amount() > 0
                && (req.expectedAmount() == null || Double.compare(req.amount(), req.expectedAmount()) == 0);

        String status = ok ? "SUCCESS" : "FAILED";
        String message = ok ? "Payment accepted" : "Amount mismatch";

        log.info("Processed payment orderId={} amount={} expectedAmount={} status={}",
                req != null ? req.orderId() : null,
                req != null ? req.amount() : null,
                req != null ? req.expectedAmount() : null,
                status);

        return ResponseEntity.ok(new PaymentResponse(req != null ? req.orderId() : null, status, message));
    }
}
