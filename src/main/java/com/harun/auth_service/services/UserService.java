package com.harun.auth_service.services;

import com.harun.auth_service.payloads.user.req.AssignRoleRequest;
import com.harun.auth_service.payloads.user.req.CreateUserRequest;
import com.harun.auth_service.payloads.user.req.UpdateUserRequest;
import com.harun.auth_service.payloads.user.req.SearchUserRequest;
import com.harun.auth_service.payloads.user.res.UserDetailResponse;
import com.harun.auth_service.payloads.user.res.UserListResponse;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface UserService {
    Page<UserListResponse> getUsers(SearchUserRequest userSearchRequest);
    UserDetailResponse getUserById(UUID id);
    UserDetailResponse getUserByEmail(String email);
    void createUser(CreateUserRequest request);
    void updateUser(UUID id, UpdateUserRequest request);
    void deleteUser(UUID id);
    void assignRole(AssignRoleRequest request);
    void detachRole(AssignRoleRequest request);
}
