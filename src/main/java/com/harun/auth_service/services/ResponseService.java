package com.harun.auth_service.services;

import com.harun.auth_service.entities.Role;
import com.harun.auth_service.entities.User;
import com.harun.auth_service.payloads.role.res.RoleResponse;
import com.harun.auth_service.payloads.user.res.UserAuthResponse;
import com.harun.auth_service.payloads.user.res.UserDetailResponse;
import com.harun.auth_service.payloads.user.res.UserListResponse;

import java.util.List;

public interface ResponseService {
    RoleResponse generateRoleResponse(Role role);
    List<RoleResponse> generateRoleResponses(User user);
    UserAuthResponse generateUserAuthResponse(User user);
    UserDetailResponse generateUserDetailResponse(User user);
    UserListResponse generateUserListResponse(User user);
}
