package com.ilogos.course.user

import java.util.UUID

interface IUser {
    val id: UUID
    val username: String
    val email: String
}
