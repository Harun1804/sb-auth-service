package com.harun.auth_service.services.impl;

import com.harun.auth_service.entities.RefreshToken;
import com.harun.auth_service.entities.User;
import com.harun.auth_service.repositories.RefreshTokenRepository;
import com.harun.auth_service.services.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * RefreshToken Service Implementation
 *
 * Manages refresh token lifecycle:
 * - Creation and storage
 * - Validation and retrieval
 * - Revocation and cleanup
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private static final int REFRESH_TOKEN_EXPIRATION_DAYS = 7;

    /**
     * Create and save a new refresh token
     */
    @Override
    public RefreshToken createRefreshToken(User user, String token, String userAgent, String ipAddress) {
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(token)
                .expiresAt(LocalDateTime.now().plusDays(REFRESH_TOKEN_EXPIRATION_DAYS))
                .userAgent(userAgent)
                .ipAddress(ipAddress)
                .build();

        RefreshToken saved = refreshTokenRepository.save(refreshToken);
        log.info("Refresh token created for user: {} (expires in {} days)", user.getEmail(), REFRESH_TOKEN_EXPIRATION_DAYS);
        return saved;
    }

    /**
     * Validate and retrieve a refresh token
     * Returns the token only if valid (not expired and not revoked)
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<RefreshToken> validateRefreshToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }

        Optional<RefreshToken> refreshToken = refreshTokenRepository.findValidByToken(token);

        if (refreshToken.isPresent()) {
            log.debug("Refresh token validated successfully for user: {}", refreshToken.get().getUser().getEmail());
        } else {
            log.warn("Invalid or expired refresh token attempt");
        }

        return refreshToken;
    }

    /**
     * Get all valid refresh tokens for a user
     * Useful for "manage active sessions" feature
     */
    @Override
    @Transactional(readOnly = true)
    public List<RefreshToken> getValidTokensByUser(UUID userId) {
        return refreshTokenRepository.findValidByUserId(userId);
    }

    /**
     * Revoke a specific refresh token (logout from one device)
     */
    @Override
    public void revokeToken(String token) {
        if (token == null || token.isBlank()) {
            return;
        }

        Optional<RefreshToken> refreshToken = refreshTokenRepository.findByToken(token);
        if (refreshToken.isPresent()) {
            refreshToken.get().revoke();
            refreshTokenRepository.save(refreshToken.get());
            log.info("Refresh token revoked for user: {}", refreshToken.get().getUser().getEmail());
        }
    }

    /**
     * Revoke all refresh tokens for a user (logout from all devices)
     * Important for security: when password is changed, token is stolen, etc.
     */
    @Override
    public void revokeAllTokensByUser(UUID userId) {
        int revokedCount = refreshTokenRepository.revokeAllByUser(userId);
        log.info("Revoked {} refresh tokens for user ID: {}", revokedCount, userId);
    }

    /**
     * Check if a token is valid without throwing exceptions
     */
    @Override
    @Transactional(readOnly = true)
    public boolean isTokenValid(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }

        Optional<RefreshToken> refreshToken = refreshTokenRepository.findValidByToken(token);
        return refreshToken.isPresent();
    }

    /**
     * Count active sessions (valid tokens) for a user
     * Useful for enforcing maximum active sessions
     */
    @Override
    @Transactional(readOnly = true)
    public long countActiveSessions(UUID userId) {
        return refreshTokenRepository.countValidByUserId(userId);
    }

    /**
     * Cleanup expired and revoked tokens (batch job)
     * Should be run periodically (e.g., daily) to free database space
     */
    @Override
    public int cleanupExpiredTokens() {
        LocalDateTime before = LocalDateTime.now().minusDays(30); // Keep 30 days of history
        int deletedCount = refreshTokenRepository.deleteAllRevokedAndExpired(before);
        log.info("Cleaned up {} expired/revoked refresh tokens", deletedCount);
        return deletedCount;
    }
}

