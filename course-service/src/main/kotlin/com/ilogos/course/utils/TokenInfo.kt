package com.ilogos.course.utils

import com.ilogos.course.user.IUser
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import java.security.PublicKey
import java.util.*

class TokenInfo(private val token: String, publicKey: PublicKey) : IUser {
    private val claims: Claims =
            Jwts.parserBuilder().setSigningKey(publicKey).build().parseClaimsJws(token).getBody()

    enum class Type {
        ACCESS,
        REFRESH,
        UNDEFINED
    }

    val isExpired: Boolean
        get() = claims.expiration.before(Date())

    val type: Type
        get() {
            val type = claims["type", String::class.java]

            return when (type) {
                "access" -> Type.ACCESS
                "refresh" -> Type.REFRESH
                else -> Type.UNDEFINED
            }
        }

    val isAccess: Boolean
        get() = type == Type.ACCESS

    val isRefresh: Boolean
        get() = type == Type.REFRESH

    val issuedAt: Date
        get() = claims.issuedAt

    override val email: String
        get() = claims["email", String::class.java]!!

    override val id: UUID
        get() = UUID.fromString(claims.subject)

    override val username: String
        get() = claims["username", String::class.java]!!
}
