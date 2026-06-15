package com.harun.auth_service.services.impl;

import com.harun.auth_service.entities.Role;
import com.harun.auth_service.payloads.user.req.*;
import com.harun.auth_service.entities.User;
import com.harun.auth_service.enums.UserStatus;
import com.harun.auth_service.payloads.user.res.UserDetailResponse;
import com.harun.auth_service.payloads.user.res.UserListResponse;
import com.harun.auth_service.repositories.RoleRepository;
import com.harun.auth_service.repositories.UserRepository;
import com.harun.auth_service.services.ResponseService;
import com.harun.auth_service.services.RoleUserService;
import com.harun.auth_service.services.UserService;
import com.harun.auth_service.utils.HashPassword;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    private final RoleUserService roleUserService;
    private final ResponseService responseService;

    private final HashPassword hashPassword;

    @Override
    public Page<UserListResponse> getUsers(SearchUserRequest userSearchRequest) {
        Specification<User> specification = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (Objects.nonNull(userSearchRequest.getKeyword())) {
                String keyword = "%" + userSearchRequest.getKeyword().toLowerCase() + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("email")), keyword)
                ));
            }

            return query.where(predicates.toArray(new Predicate[]{})).getRestriction();
        };

        Sort sort = userSearchRequest.getSortDirection().equalsIgnoreCase("asc") ? Sort.by(userSearchRequest.getSortBy()).ascending() : Sort.by(userSearchRequest.getSortBy()).descending();
        Pageable pageable = PageRequest.of(userSearchRequest.getPage(), userSearchRequest.getSize(), sort);
        Page<User> users = userRepository.findAll(specification, pageable);
        List<UserListResponse> userDetailResponse = users.getContent().stream().map(responseService::generateUserListResponse).toList();
        return new PageImpl<>(userDetailResponse, pageable, users.getTotalElements());
    }

    @Override
    public UserDetailResponse getUserById(UUID id) {
        User user = userRepository.findWithRolesById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return responseService.generateUserDetailResponse(user);
    }

    @Override
    public UserDetailResponse getUserByEmail(String email) {
        User user = userRepository.findWithRolesByEmailIgnoreCase(email).orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
        );
        return responseService.generateUserDetailResponse(user);
    }

    @Override
    @Transactional
    public void createUser(CreateUserRequest request) {
        if (userRepository.findByEmailIgnoreCase(request.email()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }

        User user = new User();
        user.setEmail(request.email());
        user.setPassword(hashPassword.generate(request.password()));
        user.setStatus(UserStatus.ACTIVE);

        userRepository.save(user);
    }

    @Override
    @Transactional
    public void updateUser(UUID id, UpdateUserRequest request) {
        User user = userRepository.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
        );

        if (!Objects.equals(user.getEmail(), request.email()) && userRepository.findByEmailIgnoreCase(request.email()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }

        user.setEmail(request.email());
        if (request.password() != null) {
            user.setPassword(hashPassword.generate(request.password()));
        }
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void deleteUser(UUID id) {
        User user = userRepository.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
        );

        userRepository.delete(user);
    }

    @Override
    public void assignRole(AssignRoleRequest request) {
        User user = userRepository.findById(request.id()).orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
        );

        List<Role> roles = roleRepository.findByIdIn(request.roles());
        if (roles.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found");
        }

        roleUserService.assignRole(new RoleUserRequest(user, roles));
    }

    @Override
    public void detachRole(AssignRoleRequest request) {
        User user = userRepository.findById(request.id()).orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
        );

        List<Role> roles = roleRepository.findByIdIn(request.roles());
        if (roles.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found");
        }

        roleUserService.detachRole(new RoleUserRequest(user, roles));
    }
}
