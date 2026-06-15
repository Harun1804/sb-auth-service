package com.harun.auth_service.payloads.user.req;

import com.harun.auth_service.entities.Role;
import com.harun.auth_service.entities.User;

import java.util.List;

public record RoleUserRequest(
    User user,
    List<Role> roles
) {
}
