package com.harun.auth_service.repositories.pivot;

import com.harun.auth_service.entities.Role;
import com.harun.auth_service.entities.User;
import com.harun.auth_service.entities.pivot.RoleUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleUserRepository extends JpaRepository<RoleUser, UUID>, JpaSpecificationExecutor<RoleUser> {
    Optional<RoleUser> findByUserAndRole(User user, Role role);
    List<RoleUser> findByRole(Role role);

    @Query("""
        SELECT ru
        FROM RoleUser ru
        JOIN FETCH ru.role
        WHERE ru.user = :user
    """)
    List<RoleUser> findByUserWithRole(User user);
}
