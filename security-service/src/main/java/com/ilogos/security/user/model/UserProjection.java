package com.ilogos.security.user.model;

import java.util.Set;

public interface UserProjection extends IUser {
    Set<String> getRoles();
}
