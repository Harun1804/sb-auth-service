package com.harun.auth_service.controllers;

import com.harun.auth_service.payloads.role.req.RoleRequest;
import com.harun.auth_service.payloads.role.req.SearchRoleRequest;
import com.harun.auth_service.payloads.role.res.RoleResponse;
import com.harun.auth_service.services.RoleService;
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
@RequestMapping("/roles")
@RequiredArgsConstructor
public class RoleController {
    private final RoleService roleService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<RoleResponse>>> index(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "sortBy", required = false, defaultValue = "id") String sortBy,
            @RequestParam(value = "sortDirection", required = false, defaultValue = "desc") String sortDirection,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") int size
    ) {
        SearchRoleRequest request = SearchRoleRequest.builder()
                .keyword(keyword)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .page(page)
                .size(size)
                .build();

        Page<RoleResponse> response = roleService.getRoles(request);
        if (response.getTotalElements() == 0) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.notFound("Role not found."));
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
    public ResponseEntity<ApiResponse<RoleResponse>> findById(@PathVariable UUID id)
    {
        RoleResponse userResponse = roleService.getRoleById(id);
        if (userResponse == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.notFound("Role not found."));
        }

        return ResponseEntity.ok(
            ApiResponse.success(userResponse, "Role found.")
        );
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> create(@Valid @RequestBody RoleRequest roleRequest) {
        roleService.createRole(roleRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(null, "Role created successfully."));
    }

    @PutMapping(value = "/{id}")
    public ResponseEntity<ApiResponse<Void>> update(@PathVariable UUID id, @Valid @RequestBody RoleRequest roleRequest) {
        roleRequest.setId(id);
        roleService.updateRole(roleRequest);
        return ResponseEntity.ok(ApiResponse.success(null, "Role updated successfully."));
    }

    @DeleteMapping(value = "/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        roleService.deleteRole(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Role deleted successfully."));
    }
}
