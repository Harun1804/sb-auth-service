package com.harun.auth_service.controllers;

import com.harun.auth_service.payloads.auth.req.LoginRequest;
import com.harun.auth_service.payloads.auth.res.LoginResponse;
import com.harun.auth_service.services.AuthService;
import com.harun.auth_service.utils.Extracting;
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

    private final Extracting extracting;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletRequest request) {
        try {
            String userAgent = request.getHeader("User-Agent");
            String ipAddress = extracting.extractClientIpAddress(request);

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
            @RequestHeader(value = "X-Refresh-Token", required = false) String refreshToken) {
        try {
            if (refreshToken == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("Refresh token is required in header"));
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

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader(value = "X-Refresh-Token", required = false) String refreshToken) {
        try {
            if (refreshToken == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("Refresh token is required in header"));
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

    @PostMapping("/logout-all")
    public ResponseEntity<ApiResponse<Void>> logoutAllDevices(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            String token = extracting.extractTokenFromHeader(authHeader);
            if (token == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("Access token is required in Authorization header"));
            }

            authService.logoutAllDevices(token);

            return ResponseEntity.status(HttpStatus.OK)
                    .body(ApiResponse.error("Logout all devices requires authentication via access token. Contact admin or use refresh token revocation."));
        } catch (Exception e) {
            log.error("Unexpected error during logout all devices", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An error occurred during logout all devices"));
        }
    }
}

