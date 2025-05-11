package com.ilogos.auth_service.user;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<UserProjection> findUserByUsername(String username);

    @EntityGraph(attributePaths = "usernameHistory")
    Optional<User> findWithUsernameHistoryByUsername(String username);

    @EntityGraph(attributePaths = "emailHistory")
    Optional<User> findWithEmailHistoryByUsername(String username);

    Optional<UserProjection> findUserByEmail(String email);

    @EntityGraph(attributePaths = "usernameHistory")
    Optional<User> findWithUsernameHistoryByEmail(String email);

    @EntityGraph(attributePaths = "emailHistory")
    Optional<User> findWithEmailHistoryByEmail(String email);

    Optional<User> findByEmailOrUsername(String email, String username);

    Optional<UserProjection> findUserByEmailOrUsername(String email, String username);

    Optional<UserProjection> findUserById(UUID id);
}
