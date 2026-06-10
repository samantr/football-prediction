package com.example.footballprediction.service;

import com.example.footballprediction.domain.Role;
import com.example.footballprediction.domain.User;
import com.example.footballprediction.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User signUp(String email, String displayName, String password) {
        String normalizedEmail = requireText(email, "Email").toLowerCase();
        String normalizedName = requireText(displayName, "Display name");
        String normalizedPassword = requireText(password, "Password");

        if (normalizedPassword.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters.");
        }

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("Email is already registered.");
        }

        User user = new User();
        user.setEmail(normalizedEmail);
        user.setDisplayName(normalizedName);
        user.setPasswordHash(passwordEncoder.encode(normalizedPassword));
        user.setRole(Role.USER);
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public User getByEmail(String email) {
        return userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
    }

    private String requireText(String value, String label) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(label + " is required.");
        }
        return value.trim();
    }
}
