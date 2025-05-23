package com.ilogos.shared.utils

import io.jsonwebtoken.Jwts
import kotlinx.datetime.*
import com.ilogos.shared.model.TokenInfo
import org.springframework.security.oauth2.jwt.Jwt
import java.security.PublicKey

actual object TokenInfoFactory {
    actual fun from(token: String, publicKey: Any): TokenInfo {
        val key = publicKey as PublicKey
        val claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).body

        val expiration = Instant.fromEpochMilliseconds(claims.expiration.time)
        val username = claims[TokenInfo.USERNAME_CLAIM, String::class.java] as? String
        val email = claims[TokenInfo.EMAIL_CLAIM, String::class.java] as? String
        val issuedAt = Instant.fromEpochMilliseconds(claims.issuedAt.time)
        val type = claims[TokenInfo.TYPE_CLAIM, String::class.java] as? String

        return TokenInfo(
            token = token,
            expiration = expiration,
            username = username,
            subject = claims.subject,
            email = email,
            issuedAt = issuedAt,
            type = type
        )
    }

    fun from(jwt: Jwt) = TokenInfo(
        jwt.tokenValue,
        Instant.fromEpochMilliseconds(jwt.expiresAt.toEpochMilli()),
        jwt.getClaimAsString(TokenInfo.USERNAME_CLAIM),
        jwt.subject,
        jwt.getClaimAsString(TokenInfo.EMAIL_CLAIM),
        Instant.fromEpochMilliseconds(jwt.issuedAt.toEpochMilli()),
        jwt.getClaimAsString(TokenInfo.TYPE_CLAIM)
    )
}
