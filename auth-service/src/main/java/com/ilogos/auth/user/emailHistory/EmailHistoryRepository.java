package com.ilogos.auth.user.emailHistory;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ilogos.auth.user.User;

public interface EmailHistoryRepository extends JpaRepository<EmailHistory, Long> {

    Optional<EmailHistory> findCurrentByUser(User user); // findByUserAndEndAtIsNull

}
