@file:Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")

package com.ilogos.shared.utils

import com.ilogos.shared.model.TokenInfo
import jsonwebtoken.JwtDecoded
import jsonwebtoken.PublicKey
import jsonwebtoken.jwt

actual object TokenInfoUtils {
    actual fun createInfo(token: String, publicKey: Any): TokenInfo {
        val key = publicKey as PublicKey
        val info = jwt.verify(token, key) as? JwtDecoded
            ?: throw RuntimeException("Invalid jwt token")

        return TokenInfo(
            token = token,
            expiration = info.exp * 1_000L,
            username = info.username,
            subject = info.sub,
            email = info.email,
            issuedAt = info.issuedAt * 1_000L,
            type = info.type
        )
    }

    fun createInfo(token: String, publicKey: PublicKey) = createInfo(token, publicKey as Any)
}
