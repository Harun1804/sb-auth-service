package com.harun.auth_service.services;

import com.harun.auth_service.payloads.user.req.CreateUserRequest;
import com.harun.auth_service.payloads.user.req.UpdateUserRequest;
import com.harun.auth_service.payloads.user.req.SearchUserRequest;
import com.harun.auth_service.payloads.user.res.UserResponse;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface UserService {
    Page<UserResponse> getUsers(SearchUserRequest userSearchRequest);
    UserResponse getUserById(UUID id);
    UserResponse getUserByEmail(String email);
    void createUser(CreateUserRequest request);
    void updateUser(UUID id, UpdateUserRequest request);
    void deleteUser(UUID id);
}
