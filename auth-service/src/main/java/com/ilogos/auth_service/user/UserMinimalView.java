package com.ilogos.auth_service.user;

import java.util.UUID;

public interface UserMinimalView {
    UUID getId();

    String getUsername();

    String getEmail();
}
