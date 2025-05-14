package com.ilogos.security.user.model;

import java.util.UUID;

public interface IUserBase {
    UUID getId();

    String getUsername();

    String getEmail();
}
