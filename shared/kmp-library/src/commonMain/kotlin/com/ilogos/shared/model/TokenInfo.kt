package com.ilogos.shared.model

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

data class TokenInfo(
    val token: String,
    val expiration: Long?,
    val username: String?,
    val subject: String,
    val email: String?,
    val issuedAt: Long?,
    val type: String?
) {

    companion object {
        const val USERNAME_CLAIM = "username"
        const val EMAIL_CLAIM = "email"
        const val TYPE_CLAIM = "type"

        const val ACCESS_TYPE = "access"
        const val REFRESH_TYPE = "refresh"

        fun isAccessType(type: String?): Boolean = type == ACCESS_TYPE
        fun isRefreshType(type: String?): Boolean = type == REFRESH_TYPE
    }

    val isExpired: Boolean get() = expiration == null || Instant.fromEpochMilliseconds(expiration) < Clock.System.now()
    val isAccessToken: Boolean get() = isAccessType(type)
    val isRefreshToken: Boolean get() = isRefreshType(type)
    val hasPayload: Boolean get() = !username.isNullOrEmpty() && !email.isNullOrEmpty() && !type.isNullOrEmpty()

}