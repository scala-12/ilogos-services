package ru.ilogos.auth_service.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RoleType {
    ROLE_STUDENT("Студент"),
    ROLE_ADMIN("Администратор"),
    ROLE_TEACHER("Преподаватель"),
    ROLE_MANAGER("Менеджер"),
    ROLE_EDITOR("Редактор курсов");

    private final String description;

}
