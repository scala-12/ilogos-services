package com.ilogos.shared.utils

import com.ilogos.shared.model.AbstractTokenInfo
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
    actual fun createInfo(token: String, publicKey: Any): AbstractTokenInfo {
        val key = publicKey as PublicKey
        val claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).body

        val expiration = claims.expiration.time
        val username = claims[AbstractTokenInfo.USERNAME_CLAIM, String::class.java]
        val email = claims[AbstractTokenInfo.EMAIL_CLAIM, String::class.java]
        val issuedAt = claims.issuedAt.time
        val type = claims[AbstractTokenInfo.TYPE_CLAIM, String::class.java]
        val roles = claims[AbstractTokenInfo.ROLES_CLAIM, List::class.java]

        return TokenInfo(
            token = token,
            expirationSec = expiration.div(1_000).toInt(),
            username = username,
            subject = claims.subject,
            email = email,
            issuedAtSec = issuedAt.div(1_000).toInt(),
            type = type,
            roles = roles?.map { it as String }?.toSet()?.toTypedArray() ?: emptyArray()
        )
    }

    fun createInfo(token: String, publicKey: PublicKey) = createInfo(token, publicKey as Any)

    fun createInfo(jwt: Jwt) = TokenInfo(
        token = jwt.tokenValue,
        expirationSec = jwt.expiresAt?.toEpochMilli()?.div(1_000)?.toInt(),
        username = jwt.getClaimAsString(AbstractTokenInfo.USERNAME_CLAIM),
        subject = jwt.subject,
        email = jwt.getClaimAsString(AbstractTokenInfo.EMAIL_CLAIM),
        issuedAtSec = jwt.issuedAt?.toEpochMilli()?.div(1_000)?.toInt(),
        type = jwt.getClaimAsString(AbstractTokenInfo.TYPE_CLAIM),
        roles = jwt.getClaimAsStringList(AbstractTokenInfo.ROLES_CLAIM).toSet().toTypedArray() ?: emptyArray()
    )

    fun buildJwtDecoder(publicKey: RSAPublicKey, logger: Consumer<String>?): JwtDecoder {
        val decoder = NimbusJwtDecoder.withPublicKey(publicKey).build()

        val tokenTypeValidator = JwtClaimValidator<String?>(AbstractTokenInfo.TYPE_CLAIM) {
            if (!AbstractTokenInfo.isAccessType(it)) {
                logger?.accept("Invalid JWT token type")
                false
            } else true
        }

        val tokenUsernameValidator = JwtClaimValidator<String?>(AbstractTokenInfo.USERNAME_CLAIM) {
            if (it.isNullOrBlank()) {
                logger?.accept("Invalid JWT token data: without username ($it)")
                false
            } else true
        }

        val tokenEmailValidator = JwtClaimValidator<String?>(AbstractTokenInfo.EMAIL_CLAIM) {
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
