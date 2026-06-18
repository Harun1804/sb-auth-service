package com.harun.auth_service.scheduled;

import com.harun.auth_service.services.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled Tasks for Token Management
 * Automatically cleanup expired and revoked tokens
 */
@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class TokenCleanupScheduler {

    private final RefreshTokenService refreshTokenService;

    /**
     * Cleanup expired and revoked refresh tokens
     * Runs daily at 2:00 AM
     * Keeps 30 days of history before deletion
     */
    @Scheduled(cron = "0 0 2 * * *") // Daily at 2:00 AM
    public void cleanupExpiredTokens() {
        try {
            log.info("Starting refresh token cleanup job...");
            int deletedCount = refreshTokenService.cleanupExpiredTokens();
            log.info("Refresh token cleanup completed. Deleted {} records", deletedCount);
        } catch (Exception e) {
            log.error("Error during refresh token cleanup", e);
        }
    }
}

