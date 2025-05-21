package com.ilogos.user.user.emailHistory;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ilogos.user.user.User;

public interface EmailHistoryRepository extends JpaRepository<EmailHistory, Long> {

    Optional<EmailHistory> findCurrentByUser(User user); // findByUserAndEndAtIsNull

}
