package com.example.userservice;

import com.example.userservice.security.JwtService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(UserService userService, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public record RegisterRequest(
            @NotBlank(message = "name is required") String name,
            @NotBlank(message = "email is required") @Email(message = "email must be valid") String email,
            @NotBlank(message = "password is required") String password,
            String roles
    ) {}

    public record LoginRequest(
            @NotBlank(message = "email is required") @Email(message = "email must be valid") String email,
            @NotBlank(message = "password is required") String password
    ) {}

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        try {
            User created = userService.register(req.name(), req.email(), req.password(), req.roles());
            return ResponseEntity.ok(Map.of(
                    "id", created.getId(),
                    "name", created.getName(),
                    "email", created.getEmail(),
                    "roles", created.getRoles()
            ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        return userService.findByEmail(req.email())
                .<ResponseEntity<?>>map(user -> {
                    if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
                        return ResponseEntity.status(401).body(Map.of("error", "invalid credentials"));
                    }

                    List<String> roles = Arrays.stream(user.getRoles().split(","))
                            .map(String::trim)
                            .filter(s -> !s.isBlank())
                            .toList();

                    String token = jwtService.issueToken(user.getId(), user.getEmail(), roles);
                    return ResponseEntity.ok(Map.of(
                            "accessToken", token,
                            "tokenType", "Bearer",
                            "expiresInSeconds", 3600,
                            "userId", user.getId(),
                            "roles", roles
                    ));
                })
                .orElseGet(() -> ResponseEntity.status(401).body(Map.of("error", "invalid credentials")));
    }
}
