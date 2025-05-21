package com.ilogos.user.user.model;

import java.util.UUID;

public interface IUserBase {
    UUID getId();

    String getUsername();

    String getEmail();
}
