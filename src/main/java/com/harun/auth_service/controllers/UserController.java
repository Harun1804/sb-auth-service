package com.harun.auth_service.controllers;

import com.harun.auth_service.payloads.user.req.AssignRoleRequest;
import com.harun.auth_service.payloads.user.req.SearchUserRequest;
import com.harun.auth_service.payloads.user.res.UserListResponse;
import com.harun.auth_service.services.UserService;
import com.harun.formatter.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.harun.auth_service.payloads.user.req.CreateUserRequest;
import com.harun.auth_service.payloads.user.req.UpdateUserRequest;
import com.harun.auth_service.payloads.user.res.UserDetailResponse;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserListResponse>>> index(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "sortBy", required = false, defaultValue = "id") String sortBy,
            @RequestParam(value = "sortDirection", required = false, defaultValue = "desc") String sortDirection,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") int size
    ) {
        SearchUserRequest request = SearchUserRequest.builder()
                .keyword(keyword)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .page(page)
                .size(size)
                .build();

        Page<UserListResponse> response = userService.getUsers(request);
        if (response.getTotalElements() == 0) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.notFound("User not found."));
        }

        return ResponseEntity.ok(
            ApiResponse.success(
                response.getContent(),
                response.getNumber(),
                response.getTotalPages(),
                response.getSize(),
                response.getTotalElements()
            )
        );
    }

    @GetMapping(value = "/find-by-id/{id}")
    public ResponseEntity<ApiResponse<UserDetailResponse>> findById(@PathVariable UUID id)
    {
        UserDetailResponse userDetailResponse = userService.getUserById(id);
        if (userDetailResponse == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.notFound("User not found."));
        }

        return ResponseEntity.ok(
            ApiResponse.success(userDetailResponse, "User found.")
        );
    }

    @GetMapping(value = "/find-by-email/{email}")
    public ResponseEntity<ApiResponse<UserDetailResponse>> findByEmail(@PathVariable String email)
    {
        UserDetailResponse userAuthResponse = userService.getUserByEmail(email);
        if (userAuthResponse == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.notFound("User not found."));
        }

        return ResponseEntity.ok(
            ApiResponse.success(userAuthResponse, "User found.")
        );
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> create(@Valid @RequestBody CreateUserRequest createUserRequest) {
        userService.createUser(createUserRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(null, "User created successfully."));
    }

    @PostMapping(value = "/assign-role")
    public ResponseEntity<ApiResponse<Void>> assignRole(@Valid @RequestBody AssignRoleRequest assignRoleRequest) {
        userService.assignRole(assignRoleRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(null, "User assigned successfully."));
    }

    @PostMapping(value = "/detach-role")
    public ResponseEntity<ApiResponse<Void>> detachRole(@Valid @RequestBody AssignRoleRequest assignRoleRequest) {
        userService.detachRole(assignRoleRequest);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(null, "User detach successfully."));
    }

    @PutMapping(value = "/{id}")
    public ResponseEntity<ApiResponse<Void>> update(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest updateUserRequest) {
        userService.updateUser(id, updateUserRequest);
        return ResponseEntity.ok(ApiResponse.success(null, "User updated successfully."));
    }

    @DeleteMapping(value = "/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success(null, "User deleted successfully."));
    }
}
