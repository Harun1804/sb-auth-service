package com.harun.auth_service.controllers;

import com.harun.auth_service.dtos.req.CreateUserRequest;
import com.harun.auth_service.dtos.req.UpdateUserRequest;
import com.harun.auth_service.dtos.req.UserSearchRequest;
import com.harun.auth_service.dtos.res.UserResponse;
import com.harun.auth_service.services.UserService;
import com.harun.formatter.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserResponse>>> index(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "sortBy", required = false, defaultValue = "id") String sortBy,
            @RequestParam(value = "sortDirection", required = false, defaultValue = "desc") String sortDirection,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") int size
    ) {
        UserSearchRequest request = UserSearchRequest.builder()
                .keyword(keyword)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .page(page)
                .size(size)
                .build();

        Page<UserResponse> response = userService.getUsers(request);
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
    public ResponseEntity<ApiResponse<UserResponse>> findById(@PathVariable UUID id)
    {
        UserResponse userResponse = userService.getUserById(id);
        if (userResponse == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.notFound("User not found."));
        }

        return ResponseEntity.ok(
            ApiResponse.success(userResponse, "User found.")
        );
    }

    @GetMapping(value = "/find-by-email/{email}")
    public ResponseEntity<ApiResponse<UserResponse>> findByEmail(@PathVariable String email)
    {
        UserResponse userResponse = userService.getUserByEmail(email);
        if (userResponse == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.notFound("User not found."));
        }

        return ResponseEntity.ok(
            ApiResponse.success(userResponse, "User found.")
        );
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> create(@Valid @RequestBody CreateUserRequest createUserRequest) {
        userService.createUser(createUserRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(null, "User created successfully."));
    }

    @PutMapping(value = "/{id}")
    public ResponseEntity<ApiResponse<Void>> update(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest updateUserRequest) {
        updateUserRequest.setId(id);
        userService.updateUser(updateUserRequest);
        return ResponseEntity.ok(ApiResponse.success(null, "User updated successfully."));
    }

    @DeleteMapping(value = "/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success(null, "User deleted successfully."));
    }
}
