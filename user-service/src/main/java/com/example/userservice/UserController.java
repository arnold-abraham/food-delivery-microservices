package com.example.userservice;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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

    public record UserResponse(Long id, String name, String email, String roles) {}

    public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {}

    private final UserService service;
    public UserController(UserService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest req) {
        User created = service.create(req.name(), req.email());
        return ResponseEntity.ok(new UserResponse(created.getId(), created.getName(), created.getEmail(), created.getRoles()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long callerId,
            @RequestHeader(value = "X-User-Roles", required = false) String roles) {
        boolean isAdmin = roles != null && roles.contains("ADMIN");
        if (!isAdmin && !id.equals(callerId)) {
            return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
        }
        return service.get(id)
                .<ResponseEntity<?>>map(u -> ResponseEntity.ok(new UserResponse(u.getId(), u.getName(), u.getEmail(), u.getRoles())))
                .orElseGet(() -> ResponseEntity.status(404).body(Map.of("error", "User not found")));
    }

    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(required = false) String role,
            @RequestHeader(value = "X-User-Roles", required = false) String callerRoles,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        if (callerRoles == null || !callerRoles.contains("ADMIN")) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }
        var pageable = PageRequest.of(page, size);
        Page<User> result = (role != null && !role.isBlank())
                ? service.listByRole(role, pageable)
                : service.listAll(pageable);
        List<UserResponse> content = result.getContent().stream()
                .map(u -> new UserResponse(u.getId(), u.getName(), u.getEmail(), u.getRoles()))
                .toList();
        return ResponseEntity.ok(new PageResponse<>(content, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages()));
    }
}
