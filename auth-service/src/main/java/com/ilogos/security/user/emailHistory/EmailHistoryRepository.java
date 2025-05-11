package com.ilogos.security.user.emailHistory;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ilogos.security.user.User;

public interface EmailHistoryRepository extends JpaRepository<EmailHistory, Long> {

    Optional<EmailHistory> findCurrentByUser(User user); // findByUserAndEndAtIsNull

}
