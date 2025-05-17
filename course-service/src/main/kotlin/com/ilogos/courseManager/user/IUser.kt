package com.ilogos.courseManager.user

import java.util.*

interface IUser {
    val id: UUID
    val username: String
    val email: String
}
