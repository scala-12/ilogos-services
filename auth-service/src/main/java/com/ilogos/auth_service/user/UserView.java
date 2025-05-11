package com.ilogos.auth_service.user;

import java.time.Instant;

public interface UserView extends UserMinimalView {
    Instant getLastTokenIssuedAt();
}
