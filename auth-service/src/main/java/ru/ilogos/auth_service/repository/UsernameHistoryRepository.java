package ru.ilogos.auth_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import ru.ilogos.auth_service.entity.UsernameHistory;

public interface UsernameHistoryRepository extends JpaRepository<UsernameHistory, Long> {

}
