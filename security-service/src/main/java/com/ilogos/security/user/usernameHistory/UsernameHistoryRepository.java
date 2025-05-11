package com.ilogos.security.user.usernameHistory;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ilogos.security.user.User;

public interface UsernameHistoryRepository extends JpaRepository<UsernameHistory, Long> {

    Optional<UsernameHistory> findCurrentByUser(User user); // findByUserAndEndAtIsNull

}
