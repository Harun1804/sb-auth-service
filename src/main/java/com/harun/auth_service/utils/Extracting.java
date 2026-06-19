package com.harun.auth_service.utils;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class Extracting {
    public String extractTokenFromHeader(String authHeader) {
        if (authHeader == null || authHeader.isBlank()) {
            return null;
        }

        String[] parts = authHeader.split(" ");
        if (parts.length != 2 || !"Bearer".equalsIgnoreCase(parts[0])) {
            return null;
        }

        return parts[1];
    }

    public String extractClientIpAddress(HttpServletRequest request) {
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
}
