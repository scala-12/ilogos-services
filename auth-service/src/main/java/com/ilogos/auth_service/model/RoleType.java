package com.ilogos.auth_service.model;

import org.springframework.security.core.GrantedAuthority;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RoleType implements GrantedAuthority {
    ROLE_STUDENT("Студент"),
    ROLE_ADMIN("Администратор"),
    ROLE_TEACHER("Преподаватель"),
    ROLE_MANAGER("Менеджер"),
    ROLE_EDITOR("Редактор курсов");

    private final String description;

    @Override
    public String getAuthority() {
        return this.name();
    }

}
