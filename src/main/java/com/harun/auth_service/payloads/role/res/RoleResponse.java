package com.harun.auth_service.payloads.role.res;

import java.util.UUID;

public record RoleResponse(
    UUID id,
    String name,
    String description
) {
}
