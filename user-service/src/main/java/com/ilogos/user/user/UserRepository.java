package com.ilogos.user.user;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.ilogos.user.common.model.IWithEmailHistory;
import com.ilogos.user.common.model.IWithUsernameHistory;

public interface UserRepository extends JpaRepository<User, UUID> {

    <T> Optional<T> findUserById(UUID id, Class<T> type);

    <T> Optional<T> findByEmail(String email, Class<T> type);

    <T> Optional<T> findByUsername(String username, Class<T> type);

    <T> Optional<T> findByEmailOrUsername(String email, String username, Class<T> type);

    @EntityGraph(attributePaths = "usernameHistory")
    <T extends IWithUsernameHistory> Optional<T> findWithUsernameHistoryByUsername(String username, Class<T> type);

    @EntityGraph(attributePaths = "usernameHistory")
    <T extends IWithUsernameHistory> Optional<T> findWithUsernameHistoryByEmail(String email, Class<T> type);

    @EntityGraph(attributePaths = "emailHistory")
    <T extends IWithEmailHistory> Optional<T> findWithEmailHistoryByUsername(String username, Class<T> type);

    @EntityGraph(attributePaths = "emailHistory")
    <T extends IWithEmailHistory> Optional<T> findWithEmailHistoryByEmail(String email, Class<T> type);
}
