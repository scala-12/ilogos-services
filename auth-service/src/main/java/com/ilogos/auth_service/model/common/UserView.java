package com.ilogos.auth_service.model.common;

import java.time.Instant;

public interface UserView extends UserMinimalView {
    Instant getLastTokenIssuedAt();
}
