package com.ilogos.shared.utils

import com.ilogos.shared.model.TokenInfo

expect object TokenInfoUtils {
    fun createInfo(token: String, publicKey: Any): TokenInfo
}