package ru.ilogos.auth_service.model.dto;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import ru.ilogos.auth_service.entity.User;
import ru.ilogos.auth_service.model.common.UserView;
import ru.ilogos.auth_service.model.projection.UserProjection;

@Getter
@AllArgsConstructor
public class UserDTO implements UserView {

    private UUID id;
    private String username;
    private String email;
    private Set<String> roles;
    private Instant lastTokenIssuedAt;

    private static UserDTO from(UserView view, Set<String> roles) {
        return new UserDTO(
                view.getId(),
                view.getUsername(),
                view.getEmail(),
                roles,
                view.getLastTokenIssuedAt());
    }

    public static UserDTO from(UserProjection projection) {
        return from(projection, projection.getRoles());
    }

    public static UserDTO from(User user) {
        return from(user, user.getRolesNames());
    }
}
