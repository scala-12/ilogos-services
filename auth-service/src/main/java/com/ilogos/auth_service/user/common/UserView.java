package com.ilogos.auth_service.user.common;

import java.time.Instant;

public interface UserView extends UserMinimalView {
    Instant getLastTokenIssuedAt();
}
