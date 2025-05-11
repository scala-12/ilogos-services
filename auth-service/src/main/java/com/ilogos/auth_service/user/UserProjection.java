package com.ilogos.auth_service.user;

import java.util.Set;

public interface UserProjection extends UserView {
    Set<String> getRoles();
}
