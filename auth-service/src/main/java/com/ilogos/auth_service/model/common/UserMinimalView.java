package com.ilogos.auth_service.model.common;

import java.util.UUID;

public interface UserMinimalView {
    UUID getId();

    String getUsername();

    String getEmail();
}
