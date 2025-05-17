package com.ilogos.user.user.usernameHistory;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ilogos.user.user.User;

public interface UsernameHistoryRepository extends JpaRepository<UsernameHistory, Long> {

    Optional<UsernameHistory> findCurrentByUser(User user); // findByUserAndEndAtIsNull

}
