package com.harun.auth_service.controllers;

import com.harun.auth_service.payloads.auth.req.LoginRequest;
import com.harun.auth_service.payloads.auth.res.LoginResponse;
import com.harun.auth_service.services.AuthService;
import com.harun.formatter.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Authentication Controller
 * Handles login and token refresh endpoints
 * Designed for high-concurrency scenarios (100+ simultaneous logins)
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletRequest request) {
        try {
            String userAgent = request.getHeader("User-Agent");
            String ipAddress = extractClientIpAddress(request);

            LoginResponse response = authService.login(loginRequest, userAgent, ipAddress);
            log.info("User login successful for email: {}", loginRequest.email());
            return ResponseEntity.ok(
                    ApiResponse.success(response, "Login successful")
            );
        } catch (IllegalArgumentException e) {
            log.warn("Login failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error during login", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An error occurred during login"));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            String refreshToken = extractTokenFromHeader(authHeader);
            if (refreshToken == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("Refresh token is required in Authorization header"));
            }

            LoginResponse response = authService.refreshAccessToken(refreshToken);
            log.info("Token refresh successful for user: {}", response.email());
            return ResponseEntity.ok(
                    ApiResponse.success(response, "Token refreshed successfully")
            );
        } catch (IllegalArgumentException e) {
            log.warn("Token refresh failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error during token refresh", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An error occurred during token refresh"));
        }
    }

    /**
     * Extract JWT token from Authorization header
     * Expected format: "Bearer <token>"
     *
     * @param authHeader Authorization header value
     * @return Token string or null if invalid format
     */
    private String extractTokenFromHeader(String authHeader) {
        if (authHeader == null || authHeader.isBlank()) {
            return null;
        }

        String[] parts = authHeader.split(" ");
        if (parts.length != 2 || !"Bearer".equalsIgnoreCase(parts[0])) {
            return null;
        }

        return parts[1];
    }

    /**
     * Extract client IP address from request
     * Handles X-Forwarded-For header for proxy compatibility
     */
    private String extractClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // X-Forwarded-For can contain multiple IPs, take the first one
            return xForwardedFor.split(",")[0].trim();
        }

        String clientIp = request.getHeader("X-Real-IP");
        if (clientIp != null && !clientIp.isEmpty()) {
            return clientIp;
        }

        return request.getRemoteAddr();
    }

    /**
     * Logout endpoint - revoke current refresh token
     * Invalidates only the current token
     *
     * @param authHeader Authorization header with refresh token
     * @return Success response
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            String refreshToken = extractTokenFromHeader(authHeader);
            if (refreshToken == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("Refresh token is required in Authorization header"));
            }

            authService.logout(refreshToken);
            log.info("User logged out successfully");
            return ResponseEntity.ok(
                    ApiResponse.success(null, "Logout successful")
            );
        } catch (IllegalArgumentException e) {
            log.warn("Logout failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error during logout", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An error occurred during logout"));
        }
    }

    /**
     * Logout from all devices endpoint
     * Revokes all refresh tokens for the user
     * Use this when:
     * - User changes password
     * - User suspects account compromise
     * - User wants to logout from all devices
     *
     * @param authHeader Authorization header with access token
     * @return Success response
     */
    @PostMapping("/logout-all")
    public ResponseEntity<ApiResponse<Void>> logoutAllDevices(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            String token = extractTokenFromHeader(authHeader);
            if (token == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("Access token is required in Authorization header"));
            }

            // You would typically extract user ID from the JWT access token
            // For now, we'll return a requirement error
            // In a real scenario, you'd use a JWT filter that extracts user info into SecurityContext

            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Logout all devices requires authentication via access token. Contact admin or use refresh token revocation."));
        } catch (Exception e) {
            log.error("Unexpected error during logout all devices", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An error occurred during logout all devices"));
        }
    }
}

