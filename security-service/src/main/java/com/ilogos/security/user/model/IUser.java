package com.ilogos.security.user.model;

import java.time.Instant;

public interface IUser extends IUserBase {
    Instant getLastTokenIssuedAt();
}
