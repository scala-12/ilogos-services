package com.ilogos.courseManager.jwt

import com.ilogos.courseManager.exception.ExceptionWithStatus
import com.ilogos.shared.model.TokenInfo
import com.ilogos.shared.utils.TokenInfoUtils
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.stereotype.Service
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.security.KeyFactory
import java.security.interfaces.RSAPublicKey
import java.security.spec.X509EncodedKeySpec
import java.util.*
import java.util.function.Consumer


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
        val pem = Files.readString(Path.of(jwtConfig.publicKeyPath))
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

    fun getTokenInfo(token: String): TokenInfo = TokenInfoUtils.createInfo(token, publicKey)

    fun extractTokenInfoFromHeader(header: String): TokenInfo {
        val token = if (header.startsWith("Bearer ")) header.removePrefix("Bearer ").trim() else ""
        if (token.isBlank()) {
            log.info("Bearer token not set")
            throw ExceptionWithStatus(HttpStatus.UNAUTHORIZED, "Bearer token not set")
        }

        return TokenInfoUtils.createInfo(token, publicKey)
    }

    fun buildJwtDecoder(): JwtDecoder {
        val logger: Consumer<String> = Consumer { msg: String -> log.info(msg) }
        return TokenInfoUtils.buildJwtDecoder(publicKey, logger)
    }
}
