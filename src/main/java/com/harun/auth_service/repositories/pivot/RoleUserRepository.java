package com.harun.auth_service.repositories.pivot;

import com.harun.auth_service.entities.Role;
import com.harun.auth_service.entities.User;
import com.harun.auth_service.entities.pivot.RoleUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleUserRepository extends JpaRepository<RoleUser, UUID>, JpaSpecificationExecutor<RoleUser> {
    Optional<RoleUser> findByUser(User user);
    Optional<RoleUser> findByUserAndRole(User user, Role role);
    List<RoleUser> findByRole(Role role);
    void deleteByUserAndRole(User user, Role role);
}
