package com.hatemnefzi.cloudsync.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.hatemnefzi.cloudsync.dto.AuthRequest;
import com.hatemnefzi.cloudsync.dto.AuthResponse;
import com.hatemnefzi.cloudsync.dto.RegisterRequest;
import com.hatemnefzi.cloudsync.service.AuthService;
import com.hatemnefzi.cloudsync.util.JwtUtil;
import com.hatemnefzi.cloudsync.entity.User;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Auth endpoint working!");
    }
     @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.SC_UNAUTHORIZED).build();
            }

            String token = authHeader.substring(7);
            if (!jwtUtil.validateToken(token)) {
                return ResponseEntity.status(HttpStatus.SC_UNAUTHORIZED).build();
            }

            String email = jwtUtil.getEmailFromToken(token);
            Long userId = jwtUtil.getUserIdFromToken(token);
            
            // Get user details from database
            User user = authService.getUserById(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("email", user.getEmail());
            response.put("fullName", user.getFullName());
            response.put("userId", user.getId());
            response.put("storageUsed", user.getStorageUsed());
            response.put("storageLimit", user.getStorageLimit());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SC_UNAUTHORIZED).build();
        }
    }
}