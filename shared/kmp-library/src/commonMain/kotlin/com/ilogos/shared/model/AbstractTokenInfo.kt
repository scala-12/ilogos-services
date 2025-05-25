package com.ilogos.shared.model

import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

@OptIn(ExperimentalJsExport::class)
@JsExport
abstract class AbstractTokenInfo(
    val token: String,
    val expirationSec: Int?,
    val username: String?,
    val subject: String,
    val email: String?,
    val issuedAtSec: Int?,
    val type: String?,
    val roles: Array<String>
) {
    companion object {
        const val USERNAME_CLAIM = "username"
        const val EMAIL_CLAIM = "email"
        const val TYPE_CLAIM = "type"
        const val ROLES_CLAIM = "roles"

        const val ACCESS_TYPE = "access"
        const val REFRESH_TYPE = "refresh"

        const val ACCESS_COOKIE_NAME = "access_token"
        const val REFRESH_COOKIE_NAME = "refresh_token"

        fun isAccessType(type: String?): Boolean = type == ACCESS_TYPE
        fun isRefreshType(type: String?): Boolean = type == REFRESH_TYPE
    }

    abstract fun isExpired(): Boolean
    val isAccessToken: Boolean get() = isAccessType(type)
    val isRefreshToken: Boolean get() = isRefreshType(type)
    val hasPayload: Boolean get() = !username.isNullOrEmpty() && !email.isNullOrEmpty() && !type.isNullOrEmpty() && !roles.isEmpty()
    fun checkUserData(username: String, email: String, roles: Array<String>): Boolean =
        hasPayload && this.username == username && this.email == email && (roles.isEmpty() || this.roles.toSet()
            .containsAll(
                roles.toSet()
            ))

}