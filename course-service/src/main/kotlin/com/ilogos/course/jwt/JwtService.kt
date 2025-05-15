package com.ilogos.course.jwt

import com.ilogos.course.exception.ExceptionWithStatus
import com.ilogos.course.utils.TokenInfo
import jakarta.annotation.PostConstruct
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.security.KeyFactory
import java.security.PublicKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.X509EncodedKeySpec
import java.util.*
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.stereotype.Service

@Service
class JwtService(private val _jwtConfig: JwtConfig?) {

    lateinit private var publicKey: PublicKey

    val jwtConfig: JwtConfig
        get() = _jwtConfig ?: throw IllegalStateException("Jwt-service has not been initialized")

    companion object {
        private val log = LoggerFactory.getLogger(JwtService::class.java)

        @Profile("test")
        fun create(publicKey: PublicKey): JwtService {
            val service = JwtService(null)
            service.publicKey = publicKey

            return service
        }
    }

    @Throws(IOException::class)
    private fun loadKey(): PublicKey {
        val pem = Files.readString(Path.of(jwtConfig.publicPath))
        if (pem.isBlank()) throw IllegalStateException("Private key is missing")

        val key =
                pem.replace("-----BEGIN PUBLIC KEY-----", "")
                        .replace("-----END PUBLIC KEY-----", "")
                        .replace("\\s".toRegex(), "")

        val decoded = Base64.getDecoder().decode(key)
        val keySpec = X509EncodedKeySpec(decoded)
        return try {
            KeyFactory.getInstance("RSA").generatePublic(keySpec)
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

    fun extractTokenInfoFromHeader(header: String): TokenInfo {
        val token = if (header.startsWith("Bearer ")) header.removePrefix("Bearer ").trim() else ""
        if (token.isBlank()) {
            log.info("Bearer token not setted")
            throw ExceptionWithStatus(HttpStatus.UNAUTHORIZED, "Bearer token not setted")
        }

        return TokenInfo(token, publicKey)
    }

    fun buildJwtDecoder(): JwtDecoder {
        val key = publicKey as RSAPublicKey
        return NimbusJwtDecoder.withPublicKey(key).build()
    }
}
