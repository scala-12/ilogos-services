package com.ilogos.shared.utils

import com.ilogos.shared.model.AbstractTokenInfo

expect object TokenInfoUtils {
    fun createInfo(token: String, publicKey: Any): AbstractTokenInfo
}