package com.ilogos.auth.jwt;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;

import com.ilogos.auth.common.TokenInfo;
import com.ilogos.auth.exception.ExceptionWithStatus;
import com.ilogos.auth.user.User;
import com.ilogos.auth.user.model.RoleType;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class JwtService {

    private final JwtConfig jwtConfig;

    private KeyPair keyPair;
    private long accessTokenExpiration;
    private long refreshTokenExpiration;

    @Profile("test")
    public static JwtService create(KeyPair keyPair, long accessTokenExpiration,
            long refreshTokenExpiration) {
        var self = new JwtService(null);
        self.init(keyPair, accessTokenExpiration, refreshTokenExpiration);

        return self;
    }

    private Key loadKey(boolean isPrivate) throws IOException {
        String pem = Files.readString(Path.of(isPrivate
                ? jwtConfig.getSecretKeyPath()
                : jwtConfig.getPublicKeyPath()));

        if (pem == null || pem.isBlank()) {
            throw new IllegalStateException("Private key is missing");
        }
        String key = (isPrivate
                ? pem.replace("-----BEGIN PRIVATE KEY-----", "")
                        .replace("-----END PRIVATE KEY-----", "")
                : pem.replace("-----BEGIN PUBLIC KEY-----", "")
                        .replace("-----END PUBLIC KEY-----", ""))
                .replaceAll("\\s", "");
        byte[] decoded = Base64.getDecoder().decode(key);
        EncodedKeySpec keySpec = isPrivate ? new PKCS8EncodedKeySpec(decoded) : new X509EncodedKeySpec(decoded);
        try {
            KeyFactory factory = KeyFactory.getInstance("RSA");
            return isPrivate
                    ? factory.generatePrivate(keySpec)
                    : factory.generatePublic(keySpec);
        } catch (Exception e) {
            throw new RuntimeException(isPrivate
                    ? "Unable to load RSA private key"
                    : "Unable to load RSA public key", e);
        }
    }

    private KeyPair loadKeys() throws IOException {
        PrivateKey privateKey = (PrivateKey) loadKey(true);
        PublicKey publicKey = (PublicKey) loadKey(false);
        return new KeyPair(publicKey, privateKey);
    }

    @PostConstruct
    public void init() throws IOException {
        if (this.jwtConfig != null) {
            var keyPair = loadKeys();
            init(keyPair, jwtConfig.getAccessTokenExpirationMs(),
                    jwtConfig.getRefreshTokenExpirationMs());
        }
    }

    private void init(KeyPair keyPair, long accessTokenExpiration,
            long refreshTokenExpiration) {
        this.keyPair = keyPair;
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    public TokenInfo getTokenInfo(String token) {
        return new TokenInfo(token, keyPair.getPublic());
    }

    public TokenInfo extractTokenInfoFromHeader(String header) {
        var token = header.startsWith("Bearer ")
                ? Optional.of(header.substring(7))
                : Optional.<String>empty();
        if (token.isEmpty()) {
            log.info("Bearer token not setted");
            throw new ExceptionWithStatus(HttpStatus.UNAUTHORIZED, "Bearer token not setted");
        }
        return new TokenInfo(token.get(), keyPair.getPublic());
    }

    public String generateToken(User user, boolean isAccess) {
        log.debug("Token generation: {}, {}", user.getUsername(), user.getId());

        long expirationMs = isAccess ? accessTokenExpiration : refreshTokenExpiration;

        Map<String, Object> claims = new HashMap<>(isAccess
                ? Map.of("roles", user.getRoles().stream().map(RoleType::name).toList())
                : Map.of());
        claims.put("username", user.getUsername());
        claims.put("email", user.getEmail());
        claims.put("type", isAccess ? "access" : "refresh");

        var token = Jwts.builder()
                .setClaims(claims)
                .setSubject(user.getId().toString())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(keyPair.getPrivate(), SignatureAlgorithm.RS256)
                .compact();

        return token;
    }

    public JwtDecoder buildJwtDecoder() {
        var key = (RSAPublicKey) keyPair.getPublic();
        var decoder = NimbusJwtDecoder.withPublicKey(key).build();

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
