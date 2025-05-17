package com.ilogos.course.utils

import com.ilogos.course.user.IUser
import io.jsonwebtoken.Jwts
import org.springframework.security.oauth2.jwt.Jwt
import java.security.PublicKey
import java.util.*

class TokenInfo : IUser {

    companion object {
        const val USERNAME_CLAIM = "username"
        const val EMAIL_CLAIM = "email"
        const val TYPE_CLAIM = "type"

        const val ACCESS_TYPE = "access"
        const val REFRESH_TYPE = "refresh"

        fun isAccessType(type: String?): Boolean = type == ACCESS_TYPE
        fun isRefreshType(type: String?): Boolean = type == REFRESH_TYPE
    }

    val token: String
    val expiration: Date
    override val username: String
    override val id: UUID
    override val email: String
    val issuedAt: Date
    val type: String

    val isExpired: Boolean get() = expiration.before(Date())
    val isAccessToken: Boolean get() = isAccessType(type)
    val isRefreshToken: Boolean get() = isRefreshType(type)

    constructor(token: String, publicKey: PublicKey) {
        this.token = token
        val claims = Jwts.parserBuilder()
            .setSigningKey(publicKey)
            .build()
            .parseClaimsJws(token)
            .body

        expiration = claims.expiration
        username = claims[USERNAME_CLAIM, String::class.java]
        id = UUID.fromString(claims.subject)
        email = claims[EMAIL_CLAIM, String::class.java]
        issuedAt = claims.issuedAt
        type = claims[TYPE_CLAIM, String::class.java]
    }

    constructor(jwt: Jwt) {
        token = jwt.tokenValue
        expiration = Date.from(jwt.expiresAt)
        username = jwt.getClaimAsString(USERNAME_CLAIM) ?: ""
        id = UUID.fromString(jwt.subject)
        email = jwt.getClaimAsString(EMAIL_CLAIM) ?: ""
        issuedAt = Date.from(jwt.issuedAt)
        type = jwt.getClaimAsString(TYPE_CLAIM) ?: ""
    }

}
