package com.ilogos.security.auth;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
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
import org.springframework.stereotype.Service;

import com.ilogos.security.exception.ExceptionWithStatus;
import com.ilogos.security.user.User;
import com.ilogos.security.user.model.RoleType;
import com.ilogos.security.utils.TokenInfo;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class AuthService {

    private final JwtConfig jwtConfig;

    private KeyPair keyPair;
    private long accessTokenExpiration;
    private long refreshTokenExpiration;

    @Profile("test")
    public static AuthService create(KeyPair keyPair, long accessTokenExpiration,
            long refreshTokenExpiration) {
        var self = new AuthService(null);
        self.init(keyPair, accessTokenExpiration, refreshTokenExpiration);

        return self;
    }

    private Key loadKey(boolean isPrivate) throws IOException {
        String pem = Files.readString(Path.of(isPrivate
                ? jwtConfig.getPrivateKeyPath()
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
            throw new RuntimeException("Unable to load RSA private key", e);
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
            init(keyPair, jwtConfig.getAccessTokenExpiration(),
                    jwtConfig.getRefreshTokenExpiration());
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

}
