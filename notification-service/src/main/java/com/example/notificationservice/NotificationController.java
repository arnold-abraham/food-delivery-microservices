package com.example.notificationservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notifications")
public class NotificationController {
    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);

    private record NotificationRequest(Long orderId, String message) {}
    private record NotificationResponse(String status) {}

    @PostMapping
    public ResponseEntity<NotificationResponse> notify(@RequestBody NotificationRequest req) {
        log.info("Notification sent orderId={} message={}", req.orderId(), req.message());
        return ResponseEntity.ok(new NotificationResponse("SENT"));
    }
}
