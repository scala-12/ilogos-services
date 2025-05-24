@file:Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")

package com.ilogos.shared.utils

import jsonwebtoken.JwtDecoded
import jsonwebtoken.jwt
import kotlinx.datetime.*
import com.ilogos.shared.model.TokenInfo

actual object TokenInfoFactory {
    actual fun from(token: String, publicKey: Any): TokenInfo {
        val key = publicKey as jsonwebtoken.PublicKey
        val info = jwt.verify(token, key) as? JwtDecoded
            ?: throw RuntimeException("Invalid jwt token")

        return TokenInfo(
            token = token,
            expiration = Instant.fromEpochMilliseconds(info.exp * 1_000L),
            username = info.username,
            subject = info.sub,
            email = info.email,
            issuedAt = Instant.fromEpochMilliseconds(info.issuedAt * 1_000L),
            type = info.type
        )
    }
}
