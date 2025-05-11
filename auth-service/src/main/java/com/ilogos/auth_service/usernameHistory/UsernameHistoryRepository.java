package com.ilogos.auth_service.usernameHistory;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ilogos.auth_service.user.User;

public interface UsernameHistoryRepository extends JpaRepository<UsernameHistory, Long> {

    Optional<UsernameHistory> findCurrentByUser(User user); // findByUserAndEndAtIsNull

}
