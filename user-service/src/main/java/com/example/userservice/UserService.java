package com.example.userservice;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Backwards-compatible create used by existing flows.
     * Creates a user with default role CUSTOMER and a non-login password.
     */
    @Transactional
    public User create(String name, String email) {
        String pw = passwordEncoder.encode("change-me");
        return repository.save(new User(name, email, pw, "CUSTOMER"));
    }

    @Transactional
    public User register(String name, String email, String rawPassword, String rolesCsv) {
        if (repository.findByEmailIgnoreCase(email).isPresent()) {
            throw new IllegalArgumentException("email already registered");
        }
        String roles = (rolesCsv == null || rolesCsv.isBlank()) ? "CUSTOMER" : rolesCsv;
        String pwHash = passwordEncoder.encode(rawPassword);
        return repository.save(new User(name, email, pwHash, roles));
    }

    public Optional<User> get(Long id) { return repository.findById(id); }

    public Optional<User> findByEmail(String email) { return repository.findByEmailIgnoreCase(email); }

    public List<User> listAll() { return repository.findAll(); }
}
