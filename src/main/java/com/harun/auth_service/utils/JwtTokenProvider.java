package com.harun.auth_service.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * JWT Token Provider - Handles token generation, validation, and extraction
 * Supports stateless authentication for high-concurrency scenarios (100+ simultaneous logins)
 *
 * KEY FEATURES:
 * - Stateless: No session storage needed, each request carries its token
 * - Scalable: Can handle unlimited concurrent users
 * - Secure: Uses HMAC-SHA256 with strong secret key
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private final String jwtSecret;
    private final long jwtExpiration;
    private final long jwtRefreshExpiration;
    private final SecretKey key;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String jwtSecret,
            @Value("${jwt.expiration}") long jwtExpiration,
            @Value("${jwt.refresh-expiration}") long jwtRefreshExpiration
    ) {
        this.jwtSecret = jwtSecret;
        this.jwtExpiration = jwtExpiration;
        this.jwtRefreshExpiration = jwtRefreshExpiration;
        // Create a strong key from the secret
        this.key = Keys.hmacShaKeyFor(
            this.jwtSecret.getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * Generate JWT access token
     * @param userId User ID
     * @param email User email
     * @param roles User roles (comma-separated)
     * @return JWT token string
     */
    public String generateAccessToken(UUID userId, String email, String roles) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("email", email);
        claims.put("roles", roles);
        claims.put("type", "access");

        return createToken(claims, userId.toString(), jwtExpiration);
    }

    /**
     * Generate JWT refresh token
     * @param userId User ID
     * @param email User email
     * @return JWT refresh token string
     */
    public String generateRefreshToken(UUID userId, String email) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("email", email);
        claims.put("type", "refresh");

        return createToken(claims, userId.toString(), jwtRefreshExpiration);
    }

    /**
     * Create JWT token with custom claims
     * @param claims Token claims
     * @param subject Token subject (usually user ID)
     * @param expiration Expiration time in milliseconds
     * @return JWT token string
     */
    private String createToken(Map<String, Object> claims, String subject, long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Extract user ID from token
     * @param token JWT token
     * @return User ID
     */
    public UUID getUserIdFromToken(String token) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            return UUID.fromString(claims.getSubject());
        } catch (Exception e) {
            log.error("Error extracting user ID from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extract email from token
     * @param token JWT token
     * @return User email
     */
    public String getEmailFromToken(String token) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            return claims.get("email", String.class);
        } catch (Exception e) {
            log.error("Error extracting email from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extract token type (access or refresh)
     * @param token JWT token
     * @return Token type
     */
    public String getTokenTypeFromToken(String token) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            return claims.get("type", String.class);
        } catch (Exception e) {
            log.error("Error extracting token type from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Get expiration date from token
     * @param token JWT token
     * @return Expiration date
     */
    public Date getExpirationDateFromToken(String token) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            return claims.getExpiration();
        } catch (Exception e) {
            log.error("Error extracting expiration from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Get all claims from token
     * @param token JWT token
     * @return Claims
     */
    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Check if token is expired
     * @param token JWT token
     * @return true if expired, false otherwise
     */
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = getExpirationDateFromToken(token);
            return expiration != null && expiration.before(new Date());
        } catch (ExpiredJwtException e) {
            log.warn("JWT token is expired: {}", e.getMessage());
            return true;
        } catch (Exception e) {
            log.error("Error checking token expiration: {}", e.getMessage());
            return true;
        }
    }

    /**
     * Validate JWT token
     * @param token JWT token
     * @return true if valid, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return !isTokenExpired(token);
        } catch (ExpiredJwtException e) {
            log.warn("JWT token is expired: {}", e.getMessage());
            return false;
        } catch (UnsupportedJwtException e) {
            log.warn("JWT token is unsupported: {}", e.getMessage());
            return false;
        } catch (MalformedJwtException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        } catch (SignatureException e) {
            log.warn("JWT signature validation failed: {}", e.getMessage());
            return false;
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims string is empty: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Error validating token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Refresh access token using refresh token
     * @param refreshToken Refresh token
     * @return New access token, or null if refresh token is invalid
     */
    public String refreshAccessToken(String refreshToken) {
        try {
            if (!validateToken(refreshToken)) {
                log.warn("Invalid refresh token");
                return null;
            }

            String tokenType = getTokenTypeFromToken(refreshToken);
            if (!"refresh".equals(tokenType)) {
                log.warn("Token is not a refresh token");
                return null;
            }

            UUID userId = getUserIdFromToken(refreshToken);
            String email = getEmailFromToken(refreshToken);

            // Generate new access token with default roles
            return generateAccessToken(userId, email, "");
        } catch (Exception e) {
            log.error("Error refreshing access token: {}", e.getMessage());
            return null;
        }
    }
}

