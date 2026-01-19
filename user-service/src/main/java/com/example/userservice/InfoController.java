package com.example.userservice;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class InfoController {

    @GetMapping("/info")
    public Map<String, Object> info() {
        return Map.of(
                "service", "user-service",
                "status", "UP"
        );
    }
}

