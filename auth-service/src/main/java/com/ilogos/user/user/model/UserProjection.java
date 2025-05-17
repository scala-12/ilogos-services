package com.ilogos.user.user.model;

import java.util.Set;

public interface UserProjection extends IUser {
    Set<String> getRoles();
}
