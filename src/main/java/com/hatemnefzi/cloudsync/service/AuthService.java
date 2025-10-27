package com.hatemnefzi.cloudsync.service;


import com.hatemnefzi.cloudsync.dto.AuthRequest;
import com.hatemnefzi.cloudsync.dto.AuthResponse;
import com.hatemnefzi.cloudsync.dto.RegisterRequest;
import com.hatemnefzi.cloudsync.entity.User;
import com.hatemnefzi.cloudsync.repository.UserRepository;
import com.hatemnefzi.cloudsync.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Value("${user.default-storage-limit}")
    private Long defaultStorageLimit;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Check if user already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        // Create new user
        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .storageUsed(0L)
                .storageLimit(defaultStorageLimit)
                .build();

        user = userRepository.save(user);

        // Generate JWT token
        String token = jwtUtil.generateToken(user.getEmail(), user.getId());

        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .fullName(user.getFullName())
                .userId(user.getId())
                .storageUsed(user.getStorageUsed())
                .storageLimit(user.getStorageLimit())
                .build();
    }

    @Transactional
    public AuthResponse login(AuthRequest request) {
        // Find user by email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Invalid email or password");
        }

        // Update last login
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        // Generate JWT token
        String token = jwtUtil.generateToken(user.getEmail(), user.getId());

        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .fullName(user.getFullName())
                .userId(user.getId())
                .storageUsed(user.getStorageUsed())
                .storageLimit(user.getStorageLimit())
                .build();
    }
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}