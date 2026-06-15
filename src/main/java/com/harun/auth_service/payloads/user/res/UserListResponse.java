package com.harun.auth_service.payloads.user.res;

import java.util.UUID;

public record UserListResponse(
    UUID id,
    String email,
    String status
) {
}
