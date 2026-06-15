package com.harun.auth_service.services.impl;

import com.harun.auth_service.entities.Role;
import com.harun.auth_service.payloads.role.req.RoleRequest;
import com.harun.auth_service.payloads.role.req.SearchRoleRequest;
import com.harun.auth_service.payloads.role.res.RoleResponse;
import com.harun.auth_service.repositories.RoleRepository;
import com.harun.auth_service.services.ResponseService;
import com.harun.auth_service.services.RoleService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {
    private final RoleRepository roleRepository;

    private final ResponseService responseService;

    @Override
    public Page<RoleResponse> getRoles(SearchRoleRequest searchRoleRequest) {
        Specification<Role> specification = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (Objects.nonNull(searchRoleRequest.getKeyword())) {
                String keyword = "%" + searchRoleRequest.getKeyword().toLowerCase() + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("name")), keyword)
                ));
            }

            return query.where(predicates.toArray(new Predicate[]{})).getRestriction();
        };

        Sort sort = searchRoleRequest.getSortDirection().equalsIgnoreCase("asc") ? Sort.by(searchRoleRequest.getSortBy()).ascending() : Sort.by(searchRoleRequest.getSortBy()).descending();
        Pageable pageable = PageRequest.of(searchRoleRequest.getPage(), searchRoleRequest.getSize(), sort);
        Page<Role> roles = roleRepository.findAll(specification, pageable);
        List<RoleResponse> roleResponses = roles.getContent().stream().map(responseService::generateRoleResponse).toList();
        return new PageImpl<>(roleResponses, pageable, roles.getTotalElements());
    }

    @Override
    public RoleResponse getRoleById(UUID id) {
        Role role = roleRepository.findById(id).orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role Not Found")
        );

        return responseService.generateRoleResponse(role);
    }

    @Override
    public void createRole(RoleRequest request) {
        if (roleRepository.findByNameIgnoreCase(request.name()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Role already exists");
        }

        Role role = new Role();
        role.setName(request.name());
        role.setDescription(request.description());
        roleRepository.save(role);
    }

    @Override
    public void updateRole(UUID id, RoleRequest request) {
        Role role = roleRepository.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role Not Found")
        );

        if (!Objects.equals(role.getName(), request.name()) && roleRepository.findByNameIgnoreCase(request.name()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Role already exists");
        }

        role.setName(request.name());
        role.setDescription(request.description());
        roleRepository.save(role);
    }

    @Override
    public void deleteRole(UUID id) {
        Role role = roleRepository.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role Not Found")
        );

        roleRepository.delete(role);
    }
}
