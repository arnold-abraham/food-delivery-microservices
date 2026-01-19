package com.example.userservice;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/users")
public class UserController {
    public record CreateUserRequest(
            @NotBlank(message = "name is required") String name,
            @NotBlank(message = "email is required") @Email(message = "email must be valid") String email
    ) {}

    public record UserResponse(Long id, String name, String email) {}

    private final UserService service;
    public UserController(UserService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest req) {
        User created = service.create(req.name(), req.email());
        return ResponseEntity.ok(new UserResponse(created.getId(), created.getName(), created.getEmail()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        return service.get(id)
                .<ResponseEntity<?>>map(u -> ResponseEntity.ok(new UserResponse(u.getId(), u.getName(), u.getEmail())))
                .orElseGet(() -> ResponseEntity.status(404).body(Map.of("error", "User not found")));
    }

    @GetMapping
    public List<UserResponse> list() {
        return service.listAll().stream()
                .map(u -> new UserResponse(u.getId(), u.getName(), u.getEmail()))
                .toList();
    }
}
