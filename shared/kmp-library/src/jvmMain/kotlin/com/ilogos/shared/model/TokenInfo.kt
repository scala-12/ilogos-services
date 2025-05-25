package com.ilogos.shared.model

import java.util.*

class TokenInfo(
    token: String,
    expirationSec: Int?,
    username: String?,
    subject: String,
    email: String?,
    issuedAtSec: Int?,
    type: String?,
    roles: Array<String>
) : AbstractTokenInfo(token, expirationSec, username, subject, email, issuedAtSec, type, roles) {
    override fun isExpired() = expirationSec == null || Date().after(Date(expirationSec * 1_000L))
}