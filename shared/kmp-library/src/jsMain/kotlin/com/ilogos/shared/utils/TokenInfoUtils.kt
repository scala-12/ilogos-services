@file:Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")

package com.ilogos.shared.utils

import com.ilogos.shared.model.AbstractTokenInfo
import com.ilogos.shared.model.JsonWebToken
import com.ilogos.shared.model.JwtDecoded
import com.ilogos.shared.model.TokenInfo

@OptIn(ExperimentalJsExport::class)
@JsExport
actual object TokenInfoUtils {
    actual fun createInfo(token: String, publicKey: Any): AbstractTokenInfo {
        val info = JsonWebToken.verify(token, publicKey)

        return TokenInfo(
            token = token,
            expirationSec = info.exp,
            username = info.username,
            subject = info.sub,
            email = info.email,
            issuedAtSec = info.iat,
            type = info.type,
            roles = info.roles?.toSet()?.toTypedArray() ?: emptyArray()
        )
    }
}
