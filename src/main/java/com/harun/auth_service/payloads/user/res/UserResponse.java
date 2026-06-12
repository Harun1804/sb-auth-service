package com.harun.auth_service.payloads.user.res;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String password,
        String status
) {
}
