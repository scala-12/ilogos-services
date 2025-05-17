package com.ilogos.user.user.model;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import com.ilogos.user.user.User;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserDTO implements IUser {

    private final UUID id;
    private final String username;
    private final String email;
    private final Set<String> roles;
    private final Instant lastTokenIssuedAt;

    private static UserDTO from(IUser view, Set<String> roles) {
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
