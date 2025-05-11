package com.ilogos.security.user.common;

import java.util.UUID;

public interface UserMinimalView {
    UUID getId();

    String getUsername();

    String getEmail();
}
