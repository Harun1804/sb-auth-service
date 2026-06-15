package com.harun.auth_service.repositories;

import com.harun.auth_service.entities.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, UUID>, JpaSpecificationExecutor<Role> {
    Optional<Role> findByNameIgnoreCase(String name);
    List<Role> findByIdIn(List<UUID> ids);
}
