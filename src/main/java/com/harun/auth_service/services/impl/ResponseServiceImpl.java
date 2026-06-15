package com.harun.auth_service.services.impl;

import com.harun.auth_service.entities.Role;
import com.harun.auth_service.entities.User;
import com.harun.auth_service.entities.pivot.RoleUser;
import com.harun.auth_service.payloads.role.res.RoleResponse;
import com.harun.auth_service.payloads.user.res.UserDetailResponse;
import com.harun.auth_service.payloads.user.res.UserListResponse;
import com.harun.auth_service.services.ResponseService;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ResponseServiceImpl implements ResponseService {
    @Override
    public RoleResponse generateRoleResponse(Role role) {
        return new RoleResponse(
            role.getId(),
            role.getName(),
            role.getDescription()
        );
    }

    @Override
    public List<RoleResponse> generateRoleResponses(User user) {
        return user.getRoleUsers()
            .stream()
            .map(RoleUser::getRole)
            .map(this::generateRoleResponse)
            .toList();
    }

    @Override
    public UserListResponse generateUserListResponse(User user) {
        return new UserListResponse(
            user.getId(),
            user.getEmail(),
            user.getStatus().toString()
        );
    }

    @Override
    public UserDetailResponse generateUserDetailResponse(User user) {
        return new UserDetailResponse(
                user.getId(),
                user.getEmail(),
                user.getStatus().toString(),
                generateRoleResponses(user)
        );
    }
}
