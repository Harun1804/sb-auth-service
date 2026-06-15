package com.harun.auth_service.payloads.user.res;

import com.harun.auth_service.payloads.role.res.RoleResponse;

import java.util.List;
import java.util.UUID;

public record UserAuthResponse(
    UUID id,
    String email,
    String password,
    String status,
    List<RoleResponse> roles
) {
}
