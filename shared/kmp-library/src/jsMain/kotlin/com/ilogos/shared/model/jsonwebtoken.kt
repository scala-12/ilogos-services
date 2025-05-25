package com.ilogos.shared.model

interface JwtDecoded {
    val exp: Int
    val iat: Int?
    val username: String?
    val sub: String
    val email: String?
    val type: String?
    val roles: Array<String>?
}

@JsModule("jsonwebtoken")
@JsNonModule
external object JsonWebToken {
    fun verify(
        token: String,
        secret: dynamic,
        options: dynamic = definedExternally
    ): dynamic
}

//@JsName("require")
//external fun require(module: String): dynamic
//
//val jsonwebtoken = require("jsonwebtoken")
//
//fun checkJwt(token: String, key: Any): JwtDecoded {
//    return jsonwebtoken.verify(token, key)
//}
