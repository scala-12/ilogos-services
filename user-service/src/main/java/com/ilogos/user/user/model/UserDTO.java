package com.ilogos.user.user.model;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.ilogos.user.common.model.IWithRoleNames;
import com.ilogos.user.common.model.IWithRoles;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserDTO implements IUserBase, IWithRoleNames {

    private final UUID id;
    private final String username;
    private final String email;
    private final Set<String> roleNames;

    private static UserDTO from(IUserBase user, Set<String> roles) {
        return new UserDTO(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                roles);
    }

    private interface IUserWithRoleNames extends IUserBase, IWithRoleNames {
    }

    public static <T extends IUserBase & IWithRoles> UserDTO from(T user) {
        return from(user, user.getRoles().stream().map(RoleType::name).collect(Collectors.toSet()));
    }

    public static UserDTO from(IUserWithRoleNames user) {
        return from(user, user.getRoleNames());
    }
}
