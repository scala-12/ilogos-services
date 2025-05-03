package ru.ilogos.auth_service.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import ru.ilogos.auth_service.entity.User;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUsernameIgnoreCase(String username);

    @EntityGraph(attributePaths = "usernameHistory")
    Optional<User> findWithUsernameHistoryByUsernameIgnoreCase(String username);

    Optional<User> findByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = "usernameHistory")
    Optional<User> findWithUsernameHistoryByEmailIgnoreCase(String email);

    Optional<User> findByEmailIgnoreCaseOrUsernameIgnoreCase(String email, String username);
}
