package com.harun.auth_service.services;

import com.harun.auth_service.payloads.auth.req.LoginRequest;
import com.harun.auth_service.payloads.auth.res.LoginResponse;

import java.util.UUID;

public interface AuthService {
    LoginResponse login(LoginRequest loginRequest, String userAgent, String ipAddress);
    LoginResponse refreshAccessToken(String refreshToken);
    void logout(String refreshToken);
    void logoutAllDevices(UUID userId);
}
