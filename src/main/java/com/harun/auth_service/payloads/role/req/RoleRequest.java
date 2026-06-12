package com.harun.auth_service.payloads.role.req;

import jakarta.validation.constraints.NotBlank;

public record RoleRequest(
        @NotBlank(message = "Name is required")
        String name,
        String description
) {
}
