package com.ilogos.shared.model

import kotlin.js.Date

@OptIn(ExperimentalJsExport::class)
@JsExport
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
    override fun isExpired(): Boolean = expirationSec == null || Date.now() > expirationSec * 1_000L
}
