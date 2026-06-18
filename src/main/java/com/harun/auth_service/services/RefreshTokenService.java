package com.harun.auth_service.services;

import com.harun.auth_service.entities.RefreshToken;
import com.harun.auth_service.entities.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * RefreshToken Service Interface
 * Handles refresh token lifecycle management
 */
public interface RefreshTokenService {

    /**
     * Create and save a new refresh token
     */
    RefreshToken createRefreshToken(User user, String token, String userAgent, String ipAddress);

    /**
     * Validate and retrieve a refresh token
     * Returns the token only if valid (not expired and not revoked)
     */
    Optional<RefreshToken> validateRefreshToken(String token);

    /**
     * Get all valid refresh tokens for a user
     */
    List<RefreshToken> getValidTokensByUser(UUID userId);

    /**
     * Revoke a specific refresh token (logout from one device)
     */
    void revokeToken(String token);

    /**
     * Revoke all refresh tokens for a user (logout from all devices)
     */
    void revokeAllTokensByUser(UUID userId);

    /**
     * Check if a token is valid
     */
    boolean isTokenValid(String token);

    /**
     * Count active sessions (valid tokens) for a user
     */
    long countActiveSessions(UUID userId);

    /**
     * Cleanup expired and revoked tokens (batch job)
     */
    int cleanupExpiredTokens();
}

