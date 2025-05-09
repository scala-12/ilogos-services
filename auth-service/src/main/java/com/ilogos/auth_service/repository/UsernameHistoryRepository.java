package com.ilogos.auth_service.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ilogos.auth_service.entity.User;
import com.ilogos.auth_service.entity.UsernameHistory;

public interface UsernameHistoryRepository extends JpaRepository<UsernameHistory, Long> {

    Optional<UsernameHistory> findCurrentByUser(User user); // findByUserAndEndAtIsNull

}
