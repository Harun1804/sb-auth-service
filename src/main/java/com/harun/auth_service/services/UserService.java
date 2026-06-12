package com.harun.auth_service.services;

import com.harun.auth_service.dtos.req.CreateUserRequest;
import com.harun.auth_service.dtos.req.UpdateUserRequest;
import com.harun.auth_service.dtos.req.UserSearchRequest;
import com.harun.auth_service.dtos.res.UserResponse;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface UserService {
    Page<UserResponse> getUsers(UserSearchRequest userSearchRequest);
    UserResponse getUserById(UUID id);
    UserResponse getUserByEmail(String email);
    void createUser(CreateUserRequest request);
    void updateUser(UpdateUserRequest request);
    void deleteUser(UUID id);
}
