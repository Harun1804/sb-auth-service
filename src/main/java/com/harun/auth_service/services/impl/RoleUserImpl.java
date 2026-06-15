package com.harun.auth_service.services.impl;

import com.harun.auth_service.entities.Role;
import com.harun.auth_service.entities.User;
import com.harun.auth_service.entities.pivot.RoleUser;
import com.harun.auth_service.exception.EntityNotFoundException;
import com.harun.auth_service.payloads.user.req.RoleUserRequest;
import com.harun.auth_service.repositories.pivot.RoleUserRepository;
import com.harun.auth_service.services.RoleUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleUserImpl implements RoleUserService {
    private final RoleUserRepository roleUserRepository;

    @Override
    @Transactional
    public void assignRole(RoleUserRequest request) {
        request.roles().forEach(role -> {
            if (roleUserRepository.findByUserAndRole(request.user(), role).isPresent()) {
                return;
            }

            RoleUser roleUser = new RoleUser();
            roleUser.setUser(request.user());
            roleUser.setRole(role);
            roleUserRepository.save(roleUser);
        });
    }

    @Override
    @Transactional
    public void detachRole(RoleUserRequest request) {
        request.roles().forEach(role -> {
            RoleUser roleUser = roleUserRepository.findByUserAndRole(request.user(), role).orElseThrow(() -> new EntityNotFoundException("Role "+ role.getName() +" not found on this user"));
            roleUserRepository.delete(roleUser);
        });
    }

    @Override
    public List<RoleUser> findByUser(User user) {
        return roleUserRepository.findByUserWithRole(user);
    }

    @Override
    public List<RoleUser> findByRole(Role role) {
        return roleUserRepository.findByRole(role);
    }
}
