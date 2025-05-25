package com.ilogos.user.jwt;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Optional;
import java.util.function.Consumer;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Service;

import com.ilogos.shared.model.TokenInfo;
import com.ilogos.shared.utils.TokenInfoUtils;
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
        return TokenInfoUtils.INSTANCE.createInfo(token, publicKey);
    }

    public TokenInfo extractTokenInfoFromHeader(String header) {
        var token = header.startsWith("Bearer ")
                ? Optional.of(header.substring(7))
                : Optional.<String>empty();
        if (token.isEmpty()) {
            log.info("Bearer token not setted");
            throw new ExceptionWithStatus(HttpStatus.UNAUTHORIZED, "Bearer token not setted");
        }
        return TokenInfoUtils.INSTANCE.createInfo(token.get(), publicKey);
    }

    public JwtDecoder buildJwtDecoder() {
        Consumer<String> logger = (String msg) -> log.info(msg);
        return TokenInfoUtils.INSTANCE.buildJwtDecoder(publicKey, logger);
    }

}
