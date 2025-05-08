package ru.ilogos.auth_service.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import ru.ilogos.auth_service.entity.User;
import ru.ilogos.auth_service.model.projection.UserProjection;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<UserProjection> findUserByUsernameIgnoreCase(String username);

    @EntityGraph(attributePaths = "usernameHistory")
    Optional<User> findWithUsernameHistoryByUsernameIgnoreCase(String username);

    @EntityGraph(attributePaths = "emailHistory")
    Optional<User> findWithEmailHistoryByUsernameIgnoreCase(String username);

    Optional<UserProjection> findUserByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = "usernameHistory")
    Optional<User> findWithUsernameHistoryByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = "emailHistory")
    Optional<User> findWithEmailHistoryByEmailIgnoreCase(String email);

    Optional<User> findByEmailIgnoreCaseOrUsernameIgnoreCase(String email, String username);

    Optional<UserProjection> findUserByEmailIgnoreCaseOrUsernameIgnoreCase(String email, String username);

    Optional<UserProjection> findUserById(UUID id);
}
