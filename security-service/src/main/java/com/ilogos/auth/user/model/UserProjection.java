package com.ilogos.auth.user.model;

import java.util.Set;

public interface UserProjection extends IUser {
    Set<String> getRoles();
}
