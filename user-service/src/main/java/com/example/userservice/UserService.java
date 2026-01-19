package com.example.userservice;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    private final UserRepository repository;
    public UserService(UserRepository repository) { this.repository = repository; }

    @Transactional
    public User create(String name, String email) { return repository.save(new User(name, email)); }
    public Optional<User> get(Long id) { return repository.findById(id); }
    public List<User> listAll() { return repository.findAll(); }
}
