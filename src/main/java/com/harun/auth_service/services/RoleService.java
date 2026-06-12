package com.harun.auth_service.services;

import com.harun.auth_service.payloads.role.req.RoleRequest;
import com.harun.auth_service.payloads.role.req.SearchRoleRequest;
import com.harun.auth_service.payloads.role.res.RoleResponse;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface RoleService {
    Page<RoleResponse> getRoles(SearchRoleRequest searchRoleRequest);
    RoleResponse getRoleById(UUID id);
    void createRole(RoleRequest request);
    void updateRole(RoleRequest request);
    void deleteRole(UUID id);
}
