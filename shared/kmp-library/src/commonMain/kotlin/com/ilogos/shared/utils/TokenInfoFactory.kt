package com.ilogos.shared.utils

import com.ilogos.shared.model.TokenInfo

expect object TokenInfoFactory {
    fun from(token: String, publicKey: Any): TokenInfo
}