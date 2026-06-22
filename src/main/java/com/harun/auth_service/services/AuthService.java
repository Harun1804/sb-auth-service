package com.harun.auth_service.services;

import com.harun.auth_service.payloads.auth.req.LoginRequest;
import com.harun.auth_service.payloads.auth.req.RegisterRequest;
import com.harun.auth_service.payloads.auth.res.LoginResponse;

public interface AuthService {
    LoginResponse login(LoginRequest loginRequest, String userAgent, String ipAddress);
    LoginResponse refreshAccessToken(String refreshToken);
    void register(RegisterRequest registerRequest);
    void logout(String refreshToken);
    void logoutAllDevices(String accessToken);
}
