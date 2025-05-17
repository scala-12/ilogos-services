package com.ilogos.auth.user.model;

import java.util.UUID;

public interface IUserBase {
    UUID getId();

    String getUsername();

    String getEmail();
}
