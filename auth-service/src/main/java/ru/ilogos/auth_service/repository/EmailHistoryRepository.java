package ru.ilogos.auth_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import ru.ilogos.auth_service.entity.EmailHistory;

public interface EmailHistoryRepository extends JpaRepository<EmailHistory, Long> {

}
