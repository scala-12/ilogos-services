package com.ilogos.auth_service.user;

import java.util.Set;

import com.ilogos.auth_service.user.common.UserView;

public interface UserProjection extends UserView {
    Set<String> getRoles();
}
