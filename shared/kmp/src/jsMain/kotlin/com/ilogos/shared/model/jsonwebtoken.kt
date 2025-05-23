@file:JsModule("jsonwebtoken")
@file:JsNonModule

package jsonwebtoken

external interface PublicKey {}

external interface JwtPayload {
    val exp: Int?
    val iat: Int?
    val username: String?
    val sub: String?
    val email: String?
    val type: String?
}

external interface JwtDecoded {
    val exp: Long
    val issuedAt: Long
    val username: String?
    val sub: String
    val email: String?
    val type: String?
}

external object jwt {
    fun verify(token: String, key: PublicKey): JwtPayload
}
