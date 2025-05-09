package com.ilogos.auth_service.model.projection;

import java.util.Set;

import com.ilogos.auth_service.model.common.UserView;

public interface UserProjection extends UserView {
    Set<String> getRoles();
}
