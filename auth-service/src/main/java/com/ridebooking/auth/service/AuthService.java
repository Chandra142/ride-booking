package com.ridebooking.auth.service;

import com.ridebooking.auth.dto.AuthResponse;
import com.ridebooking.auth.dto.LoginRequest;
import com.ridebooking.auth.dto.RegisterRequest;
import com.ridebooking.auth.entity.Role;
import com.ridebooking.auth.entity.User;
import com.ridebooking.auth.repository.UserRepository;
import com.ridebooking.auth.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * ============================================================
 * AuthService
 * ============================================================
 *
 * This service contains all authentication-related business logic.
 *
 * Responsibilities:
 * 1. Register new users.
 * 2. Authenticate existing users.
 * 3. Generate JWT tokens after successful login.
 *
 * Controller ---> AuthService ---> Repository ---> Database
 *
 * The controller should never directly access the repository.
 * All business logic belongs in this service layer.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    /**
     * Registers a new user.
     *
     * Workflow:
     * Request
     *   ↓
     * Validate Email
     *   ↓
     * Encrypt Password
     *   ↓
     * Save User
     *   ↓
     * Return Success Response
     *
     * @param request User registration details.
     * @return Success response.
     */
    public AuthResponse register(RegisterRequest request) {

        // Check if email is already registered
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists.");
        }

        // Create a new user object
        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole() == null ? Role.USER : request.getRole())
                .build();

        // Save user into PostgreSQL
        userRepository.save(user);

        return AuthResponse.builder()
                .message("User registered successfully.")
                .build();
    }

    /**
     * Authenticates an existing user.
     *
     * Workflow:
     * Login Request
     *      ↓
     * Verify Email & Password
     *      ↓
     * Generate JWT Token
     *      ↓
     * Return Token
     *
     * @param request Login credentials.
     * @return JWT token.
     */
    public AuthResponse login(LoginRequest request) {

        // Spring Security verifies email and password
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        // Look up user to get ID and role for JWT claims
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found after authentication"));

        // Generate JWT with ID, email, and role claims
        String token = jwtService.generateToken(user.getEmail(), user.getId(), user.getRole().name());

        return AuthResponse.builder()
                .token(token)
                .message("Login successful.")
                .build();
    }

}