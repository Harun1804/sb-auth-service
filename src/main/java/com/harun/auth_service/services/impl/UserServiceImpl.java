package com.harun.auth_service.services.impl;

import com.harun.auth_service.payloads.user.req.CreateUserRequest;
import com.harun.auth_service.payloads.user.req.UpdateUserRequest;
import com.harun.auth_service.payloads.user.req.SearchUserRequest;
import com.harun.auth_service.entities.User;
import com.harun.auth_service.enums.UserStatus;
import com.harun.auth_service.payloads.user.res.UserResponse;
import com.harun.auth_service.repositories.UserRepository;
import com.harun.auth_service.services.UserService;
import com.harun.auth_service.utils.HashPassword;
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
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final HashPassword hashPassword;

    @Override
    public Page<UserResponse> getUsers(SearchUserRequest userSearchRequest) {
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
        List<UserResponse> userResponses = users.getContent().stream().map(user -> generateUserResponse(user, false)).toList();
        return new PageImpl<>(userResponses, pageable, users.getTotalElements());
    }

    @Override
    public UserResponse getUserById(UUID id) {
        User user = userRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return generateUserResponse(user, false);
    }

    @Override
    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmailIgnoreCase(email).orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
        );
        return generateUserResponse(user, true);
    }

    @Override
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
    public void deleteUser(UUID id) {
        User user = userRepository.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
        );

        userRepository.delete(user);
    }

    private UserResponse generateUserResponse(User user, Boolean needPassword) {
        return new UserResponse(
            user.getId(),
            user.getEmail(),
            needPassword ? user.getPassword() : null,
            user.getStatus().toString()
        );
    }
}
