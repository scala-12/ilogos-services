package com.ilogos.user.common.model;

import java.util.Set;

import com.ilogos.user.user.model.RoleType;

public interface IWithRoles {
    Set<RoleType> getRoles();
}
