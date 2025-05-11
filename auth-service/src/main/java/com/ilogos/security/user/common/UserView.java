package com.ilogos.security.user.common;

import java.time.Instant;

public interface UserView extends UserMinimalView {
    Instant getLastTokenIssuedAt();
}
