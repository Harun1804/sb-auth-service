package com.harun.auth_service.services.impl;

import com.harun.auth_service.entities.User;
import com.harun.auth_service.enums.UserStatus;
import com.harun.auth_service.exception.EntityNotFoundException;
import com.harun.auth_service.payloads.auth.req.LoginRequest;
import com.harun.auth_service.payloads.auth.res.LoginResponse;
import com.harun.auth_service.repositories.UserRepository;
import com.harun.auth_service.services.AuthService;
import com.harun.auth_service.services.RefreshTokenService;
import com.harun.auth_service.utils.HashPassword;
import com.harun.auth_service.utils.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Authentication Service Implementation
 *
 * DESIGN FOR HIGH CONCURRENCY (100+ simultaneous logins):
 * 1. Uses JWT (stateless) - no session storage needed
 * 2. Minimal database queries with @EntityGraph optimization
 * 3. Efficient role loading with lazy loading strategy
 * 4. Thread-safe password comparison via Spring Security
 * 5. No locking/synchronization needed on read operations
 *
 * SCALABILITY FEATURES:
 * - Stateless authentication (no server-side storage)
 * - Token-based approach (no session replication needed)
 * - Can be scaled horizontally without session affinity
 * - Database queries optimized with @EntityGraph
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final HashPassword hashPassword;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    @Override
    @Transactional
    public LoginResponse login(LoginRequest loginRequest, String userAgent, String ipAddress) {
        // Validate input
        if (loginRequest.email() == null || loginRequest.password() == null) {
            log.warn("Login attempt with missing credentials");
            throw new IllegalArgumentException("Email and password are required");
        }

        String email = loginRequest.email().toLowerCase().trim();
        String password = loginRequest.password();

        // Fetch user with roles in single query (optimized with @EntityGraph)
        User user = userRepository.findWithRolesByEmailIgnoreCase(email)
                .orElseThrow(() -> {
                    log.warn("Login attempt for non-existent user: {}", email);
                    return new EntityNotFoundException("User not found. Email: " + email);
                });

        // Validate user status
        if (user.getStatus() == UserStatus.SUSPENDED) {
            log.warn("Login attempt for suspended user: {}", email);
            throw new IllegalArgumentException("User account is suspended. Please contact support.");
        }

        if (user.getStatus() == UserStatus.PENDING) {
            log.warn("Login attempt for pending user: {}", email);
            throw new IllegalArgumentException("User account is not yet activated. Please check your email.");
        }

        // Verify password
        if (!hashPassword.check(password, user.getPassword())) {
            log.warn("Failed login attempt for user: {}", email);
            throw new IllegalArgumentException("Invalid email or password");
        }

        // Extract roles from user-role relationship
        List<String> roles = user.getRoleUsers()
                .stream()
                .map(roleUser -> roleUser.getRole().getName())
                .collect(Collectors.toList());

        // Generate JWT tokens
        String rolesString = String.join(",", roles);
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail(), rolesString);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId(), user.getEmail());

        // Save refresh token to database for revocation capability
        refreshTokenService.createRefreshToken(user, refreshToken, userAgent, ipAddress);

        log.info("User logged in successfully: {} (roles: {})", email, rolesString);

        // Return login response
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(86400) // 24 hours in seconds
                .roles(roles)
                .email(email)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponse refreshAccessToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            log.warn("Refresh token attempt with missing token");
            throw new IllegalArgumentException("Refresh token is required");
        }

        refreshToken = refreshToken.trim();

        // Check if refresh token exists and is valid in database (not revoked)
        if (!refreshTokenService.isTokenValid(refreshToken)) {
            log.warn("Refresh token not found in database or has been revoked");
            throw new IllegalArgumentException("Invalid or revoked refresh token");
        }

        // Validate JWT signature and structure
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            log.warn("Invalid or expired refresh token");
            throw new IllegalArgumentException("Invalid or expired refresh token");
        }

        String tokenType = jwtTokenProvider.getTokenTypeFromToken(refreshToken);
        if (!"refresh".equals(tokenType)) {
            log.warn("Token is not a refresh token");
            throw new IllegalArgumentException("Token must be a refresh token");
        }

        UUID userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        String email = jwtTokenProvider.getEmailFromToken(refreshToken);

        // Generate new access token without database query
        String newAccessToken = jwtTokenProvider.refreshAccessToken(refreshToken);

        if (newAccessToken == null) {
            log.error("Failed to generate new access token from refresh token");
            throw new IllegalArgumentException("Failed to generate new access token");
        }

        log.info("Access token refreshed for user: {}", email);

        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(86400) // 24 hours in seconds
                .roles(List.of())
                .email(email)
                .build();
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            log.warn("Logout attempt with missing refresh token");
            throw new IllegalArgumentException("Refresh token is required");
        }

        refreshTokenService.revokeToken(refreshToken);
        log.info("User logged out successfully");
    }

    @Override
    @Transactional
    public void logoutAllDevices(UUID userId) {
        if (userId == null) {
            log.warn("Logout all devices attempt with missing user ID");
            throw new IllegalArgumentException("User ID is required");
        }

        refreshTokenService.revokeAllTokensByUser(userId);
        log.info("User logged out from all devices: {}", userId);
    }
}


