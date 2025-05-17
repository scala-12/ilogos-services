package com.ilogos.user.user.model;

import java.time.Instant;

public interface IUser extends IUserBase {
    Instant getLastTokenIssuedAt();
}
