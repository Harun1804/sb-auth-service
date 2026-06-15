package com.harun.auth_service.utils;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class HashPassword {
    private final PasswordEncoder passwordEncoder;

    public HashPassword(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public String generate(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    public boolean check(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
}
