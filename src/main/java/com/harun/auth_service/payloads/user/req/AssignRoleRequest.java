package com.harun.auth_service.payloads.user.req;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record AssignRoleRequest(
    @NotNull(message = "User ID is required")
    UUID id,

    @NotNull(message = "Role ids is required")
    List<UUID> roles
) {
}
