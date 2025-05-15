package com.ilogos.course.user;

import java.util.UUID;

public interface IUser {
    UUID getId();

    String getUsername();

    String getEmail();
}
