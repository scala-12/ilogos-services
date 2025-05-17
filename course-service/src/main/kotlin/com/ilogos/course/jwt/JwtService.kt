package com.ilogos.course.jwt

import com.ilogos.course.exception.ExceptionWithStatus
import com.ilogos.course.utils.TokenInfo
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtClaimValidator
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.stereotype.Service
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.security.KeyFactory
import java.security.interfaces.RSAPublicKey
import java.security.spec.X509EncodedKeySpec
import java.util.*


@Service
class JwtService(private val _jwtConfig: JwtConfig?) {

    private lateinit var publicKey: RSAPublicKey

    val jwtConfig: JwtConfig
        get() = _jwtConfig ?: throw IllegalStateException("Jwt-service has not been initialized")

    companion object {
        private val log = LoggerFactory.getLogger(JwtService::class.java)

        @Profile("test")
        fun create(publicKey: RSAPublicKey): JwtService {
            val service = JwtService(null)
            service.publicKey = publicKey

            return service
        }
    }

    @Throws(IOException::class)
    private fun loadKey(): RSAPublicKey {
        val pem = Files.readString(Path.of(jwtConfig.publicPath))
        if (pem.isBlank()) throw IllegalStateException("Private key is missing")

        val key = pem.replace("-----BEGIN PUBLIC KEY-----", "").replace("-----END PUBLIC KEY-----", "")
            .replace("\\s".toRegex(), "")

        val decoded = Base64.getDecoder().decode(key)
        val keySpec = X509EncodedKeySpec(decoded)
        return try {
            KeyFactory.getInstance("RSA").generatePublic(keySpec) as RSAPublicKey
        } catch (e: Exception) {
            throw RuntimeException("Unable to load RSA key", e)
        }
    }

    @PostConstruct
    @Throws(IOException::class)
    fun init() {
        if (_jwtConfig != null) {
            val key = loadKey()
            publicKey = key
        }
    }

    fun getTokenInfo(token: String): TokenInfo = TokenInfo(token, publicKey)

    fun extractTokenInfoFromHeader(header: String): TokenInfo {
        val token = if (header.startsWith("Bearer ")) header.removePrefix("Bearer ").trim() else ""
        if (token.isBlank()) {
            log.info("Bearer token not set")
            throw ExceptionWithStatus(HttpStatus.UNAUTHORIZED, "Bearer token not set")
        }

        return TokenInfo(token, publicKey)
    }

    fun buildJwtDecoder(): JwtDecoder {
        val decoder = NimbusJwtDecoder.withPublicKey(publicKey).build()

        val tokenTypeValidator = JwtClaimValidator<String?>(TokenInfo.TYPE_CLAIM) {
            if (!TokenInfo.isAccessType(it)) {
                log.info("Attempt to gain access via refresh token")
                false
            } else true
        }

        val tokenUsernameValidator = JwtClaimValidator<String?>(TokenInfo.USERNAME_CLAIM) {
            if (it.isNullOrBlank()) {
                log.info("Attempt to gain access via token without username ($it)")
                false
            } else true
        }

        val tokenEmailValidator = JwtClaimValidator<String?>(TokenInfo.EMAIL_CLAIM) {
            if (it?.contains("@") != true) {
                log.info("Attempt to gain access via token with incorrect email ($it)")
                false
            } else true
        }

        val validator = DelegatingOAuth2TokenValidator<Jwt>(
            tokenTypeValidator,
            tokenUsernameValidator,
            tokenEmailValidator
        )

        decoder.setJwtValidator(validator)

        return decoder
    }
}
