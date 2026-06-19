package com.harun.auth_service.repositories;

import com.harun.auth_service.entities.RefreshToken;
import com.harun.auth_service.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * RefreshToken Repository
 * Handles CRUD operations and queries for refresh tokens
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /**
     * Find a valid (not expired, not revoked) refresh token by token string
     */
    @Query("""
        SELECT rt FROM RefreshToken rt 
        WHERE rt.tokenHash = :token 
        AND rt.revokedAt IS NULL 
        AND rt.expiresAt > CURRENT_TIMESTAMP
    """)
    Optional<RefreshToken> findValidByToken(@Param("token") String token);

    /**
     * Find a refresh token by token string (regardless of validity)
     */
    Optional<RefreshToken> findByTokenHash(String token);

    /**
     * Find all valid refresh tokens for a user
     */
    @Query("""
        SELECT rt FROM RefreshToken rt 
        WHERE rt.user = :user 
        AND rt.revokedAt IS NULL 
        AND rt.expiresAt > CURRENT_TIMESTAMP
    """)
    List<RefreshToken> findValidByUser(@Param("user") User user);

    /**
     * Find all refresh tokens for a user (including revoked and expired)
     */
    List<RefreshToken> findByUser(User user);

    /**
     * Find all valid refresh tokens for a user by ID
     */
    @Query("""
        SELECT rt FROM RefreshToken rt 
        WHERE rt.user.id = :userId 
        AND rt.revokedAt IS NULL 
        AND rt.expiresAt > CURRENT_TIMESTAMP
    """)
    List<RefreshToken> findValidByUserId(@Param("userId") UUID userId);

    /**
     * Revoke all refresh tokens for a user (logout from all devices)
     */
    @Modifying
    @Query("""
        UPDATE RefreshToken rt 
        SET rt.revokedAt = CURRENT_TIMESTAMP 
        WHERE rt.user.id = :userId 
        AND rt.revokedAt IS NULL
    """)
    int revokeAllByUser(@Param("userId") UUID userId);

    /**
     * Revoke all expired tokens (cleanup job)
     */
    @Modifying
    @Query("""
        UPDATE RefreshToken rt 
        SET rt.revokedAt = CURRENT_TIMESTAMP 
        WHERE rt.expiresAt <= CURRENT_TIMESTAMP 
        AND rt.revokedAt IS NULL
    """)
    int revokeAllExpiredTokens();

    /**
     * Delete all revoked and expired tokens (cleanup job)
     */
    @Modifying
    @Query("""
        DELETE FROM RefreshToken rt 
        WHERE (rt.revokedAt IS NOT NULL OR rt.expiresAt <= CURRENT_TIMESTAMP)
        AND rt.updatedAt < :before
    """)
    int deleteAllRevokedAndExpired(@Param("before") LocalDateTime before);

    /**
     * Count valid refresh tokens for a user
     */
    @Query("""
        SELECT COUNT(rt) FROM RefreshToken rt 
        WHERE rt.user.id = :userId 
        AND rt.revokedAt IS NULL 
        AND rt.expiresAt > CURRENT_TIMESTAMP
    """)
    long countValidByUserId(@Param("userId") UUID userId);
}

