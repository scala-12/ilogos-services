package com.ilogos.course.jwt;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;

import com.ilogos.course.exception.ExceptionWithStatus;
import com.ilogos.course.utils.TokenInfo;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class JwtService {

    private final JwtConfig jwtConfig;

    private PublicKey publicKey;

    @Profile("test")
    public static JwtService create(PublicKey publicKey) {
        var self = new JwtService(null);
        self.init(publicKey);

        return self;
    }

    private PublicKey loadKey() throws IOException {
        String pem = Files.readString(Path.of(jwtConfig.getPublicKeyPath()));

        if (pem == null || pem.isBlank()) {
            throw new IllegalStateException("Private key is missing");
        }
        String key = (pem.replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", ""))
                .replaceAll("\\s", "");
        byte[] decoded = Base64.getDecoder().decode(key);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decoded);
        try {
            KeyFactory factory = KeyFactory.getInstance("RSA");
            return factory.generatePublic(keySpec);
        } catch (Exception e) {
            throw new RuntimeException("Unable to load RSA key", e);
        }
    }

    @PostConstruct
    public void init() throws IOException {
        if (this.jwtConfig != null) {
            var publicKey = loadKey();
            init(publicKey);
        }
    }

    private void init(PublicKey publicKey) {
        this.publicKey = publicKey;
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
        return NimbusJwtDecoder.withPublicKey((RSAPublicKey) publicKey).build();
    }

}
