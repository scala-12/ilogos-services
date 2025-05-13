package com.ilogos.security.user;

import java.util.Set;

import com.ilogos.security.user.common.UserView;

public interface UserProjection extends UserView {
    Set<String> getRoles();
}
