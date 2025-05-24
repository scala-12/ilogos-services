package com.ilogos.shared.utils

import com.ilogos.shared.model.TokenInfo
import io.jsonwebtoken.Jwts
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtClaimValidator
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import java.security.PublicKey
import java.security.interfaces.RSAPublicKey
import java.util.function.Consumer

actual object TokenInfoUtils {
    actual fun createInfo(token: String, publicKey: Any): TokenInfo {
        val key = publicKey as PublicKey
        val claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).body

        val expiration = claims.expiration.time
        val username = claims[TokenInfo.USERNAME_CLAIM, String::class.java]
        val email = claims[TokenInfo.EMAIL_CLAIM, String::class.java]
        val issuedAt = claims.issuedAt.time
        val type = claims[TokenInfo.TYPE_CLAIM, String::class.java]

        return TokenInfo(
            token = token,
            expiration = expiration,
            username = username,
            subject = claims.subject,
            email = email,
            issuedAt = issuedAt,
            type = type
        )
    }

    fun createInfo(token: String, publicKey: PublicKey) = createInfo(token, publicKey as Any)

    fun createInfo(jwt: Jwt) = TokenInfo(
        jwt.tokenValue,
        jwt.expiresAt?.toEpochMilli(),
        jwt.getClaimAsString(TokenInfo.USERNAME_CLAIM),
        jwt.subject,
        jwt.getClaimAsString(TokenInfo.EMAIL_CLAIM),
        jwt.issuedAt?.toEpochMilli(),
        jwt.getClaimAsString(TokenInfo.TYPE_CLAIM)
    )

    fun buildJwtDecoder(publicKey: RSAPublicKey, logger: Consumer<String>?): JwtDecoder {
        val decoder = NimbusJwtDecoder.withPublicKey(publicKey).build()

        val tokenTypeValidator = JwtClaimValidator<String?>(TokenInfo.TYPE_CLAIM) {
            if (!TokenInfo.isAccessType(it)) {
                logger?.accept("Invalid JWT token type")
                false
            } else true
        }

        val tokenUsernameValidator = JwtClaimValidator<String?>(TokenInfo.USERNAME_CLAIM) {
            if (it.isNullOrBlank()) {
                logger?.accept("Invalid JWT token data: without username ($it)")
                false
            } else true
        }

        val tokenEmailValidator = JwtClaimValidator<String?>(TokenInfo.EMAIL_CLAIM) {
            if (it?.contains("@") != true) {
                logger?.accept("Invalid JWT token: incorrect email ($it)")
                false
            } else true
        }

        val validator = DelegatingOAuth2TokenValidator<Jwt>(
            tokenTypeValidator, tokenUsernameValidator, tokenEmailValidator
        )

        decoder.setJwtValidator(validator)

        return decoder
    }

}
