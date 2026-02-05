package com.example.paymentservice;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments")
public class PaymentController {
    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private record PaymentRequest(
            @NotNull(message = "orderId is required") @Min(value = 1, message = "orderId must be >= 1") Long orderId,
            @NotNull(message = "amount is required") @Min(value = 1, message = "amount must be >= 1") Double amount,
            Double expectedAmount
    ) {}

    private record PaymentResponse(Long orderId, String status, String message) {}

    @PostMapping
    public ResponseEntity<PaymentResponse> pay(@Valid @RequestBody PaymentRequest req) {
        boolean ok = req.amount() > 0
                && (req.expectedAmount() == null || Double.compare(req.amount(), req.expectedAmount()) == 0);

        String status = ok ? "SUCCESS" : "FAILED";
        String message = ok ? "Payment accepted" : "Amount mismatch";

        log.info("Processed payment orderId={} amount={} expectedAmount={} status={}",
                req.orderId(),
                req.amount(),
                req.expectedAmount(),
                status);

        return ResponseEntity.ok(new PaymentResponse(req.orderId(), status, message));
    }
}
