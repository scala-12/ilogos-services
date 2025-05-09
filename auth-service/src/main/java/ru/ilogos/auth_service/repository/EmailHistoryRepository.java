package ru.ilogos.auth_service.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ru.ilogos.auth_service.entity.EmailHistory;
import ru.ilogos.auth_service.entity.User;

public interface EmailHistoryRepository extends JpaRepository<EmailHistory, Long> {

    Optional<EmailHistory> findCurrentByUser(User user); // findByUserAndEndAtIsNull

}
