package com.ilogos.user.jwt;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;

import com.ilogos.user.common.TokenInfo;
import com.ilogos.user.exception.ExceptionWithStatus;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class JwtService {

    private final JwtConfig jwtConfig;

    private RSAPublicKey publicKey;

    private RSAPublicKey loadKey() throws IOException {
        String pem = Files.readString(Path.of(jwtConfig.getPublicKeyPath()));

        if (pem == null || pem.isBlank()) {
            throw new IllegalStateException("Public key is missing");
        }
        String key = pem.replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        byte[] decoded = Base64.getDecoder().decode(key);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decoded);
        try {
            KeyFactory factory = KeyFactory.getInstance("RSA");
            return (RSAPublicKey) factory.generatePublic(keySpec);
        } catch (Exception e) {
            throw new RuntimeException("Unable to load RSA public key", e);
        }
    }

    @PostConstruct
    public void init() throws IOException {
        if (this.jwtConfig != null) {
            publicKey = loadKey();
        }
    }

    public TokenInfo getTokenInfo(String token) {
        return new TokenInfo(token, publicKey);
    }

    public TokenInfo extractTokenInfoFromHeader(String header) {
        var token = header.startsWith("Bearer ")
                ? Optional.of(header.substring(7))
                : Optional.<String>empty();
        if (token.isEmpty()) {
            log.info("Bearer token not setted");
            throw new ExceptionWithStatus(HttpStatus.UNAUTHORIZED, "Bearer token not setted");
        }
        return new TokenInfo(token.get(), publicKey);
    }

    public JwtDecoder buildJwtDecoder() {
        var decoder = NimbusJwtDecoder.withPublicKey(publicKey).build();

        JwtClaimValidator<String> tokenTypeValidator = new JwtClaimValidator<>(TokenInfo.TYPE_CLAIM,
                it -> {
                    if (!TokenInfo.isAccessType(it)) {
                        log.info("Attempt to gain access via refresh token");
                        return false;
                    }
                    return true;
                });

        JwtClaimValidator<String> tokenUsernameValidator = new JwtClaimValidator<>(TokenInfo.USERNAME_CLAIM,
                it -> {
                    if (it == null || it.isBlank()) {
                        log.info("Attempt to gain access via token without username (%s)".formatted(it));
                        return false;
                    }
                    return true;
                });

        JwtClaimValidator<String> tokenEmailValidator = new JwtClaimValidator<>(TokenInfo.EMAIL_CLAIM,
                it -> {
                    if (it == null || !it.contains("@")) {
                        log.info("Attempt to gain access via token with incorrect email (%s)".formatted(it));
                        return false;
                    }
                    return true;
                });

        OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(
                tokenTypeValidator,
                tokenUsernameValidator,
                tokenEmailValidator);

        decoder.setJwtValidator(validator);

        return decoder;
    }

}
