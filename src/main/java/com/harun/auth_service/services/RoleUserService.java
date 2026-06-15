package com.harun.auth_service.services;

import com.harun.auth_service.entities.Role;
import com.harun.auth_service.entities.User;
import com.harun.auth_service.entities.pivot.RoleUser;
import com.harun.auth_service.payloads.user.req.RoleUserRequest;

import java.util.List;

public interface RoleUserService {
    void assignRole(RoleUserRequest request);
    void detachRole(RoleUserRequest request);
    List<RoleUser> findByUser(User user);
    List<RoleUser> findByRole(Role role);
}
