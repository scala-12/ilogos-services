package ru.ilogos.auth_service.service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.ilogos.auth_service.config.JwtConfig;
import ru.ilogos.auth_service.entity.User;
import ru.ilogos.auth_service.model.RoleType;
import ru.ilogos.auth_service.model.TokenInfo;

@Slf4j
@RequiredArgsConstructor
@Service
public class JwtService {

    private final JwtConfig jwtConfig;

    private Key secretKey;
    private long accessTokenExpiration;
    private long refreshTokenExpiration;

    @Profile("test")
    public static JwtService create(String secretKey, long accessTokenExpiration, long refreshTokenExpiration) {
        var self = new JwtService(null);
        self.secretKey = Keys.hmacShaKeyFor(secretKey.getBytes());
        self.accessTokenExpiration = accessTokenExpiration;
        self.refreshTokenExpiration = refreshTokenExpiration;

        return self;
    }

    @PostConstruct
    public void init() {
        if (this.jwtConfig != null) {
            this.secretKey = Keys.hmacShaKeyFor(jwtConfig.getSecretKey().getBytes());
            this.accessTokenExpiration = jwtConfig.getAccessTokenExpiration();
            this.refreshTokenExpiration = jwtConfig.getRefreshTokenExpiration();
        }
    }

    public TokenInfo getTokenInfo(String token) {
        return new TokenInfo(token, secretKey);
    }

    public String generateAccessToken(User user) {
        return generateToken(
                Map.of("roles", user.getRoles().stream().map(RoleType::name).toList()),
                user,
                accessTokenExpiration);
    }

    public String generateRefreshToken(User user) {
        return generateToken(Map.of(), user, refreshTokenExpiration);
    }

    private String generateToken(Map<String, Object> extraClaims, User user, long expirationMs) {
        log.debug("Token generation: {}, {}", user.getUsername(), user.getId());

        Map<String, Object> claims = new HashMap<>(extraClaims);
        claims.put("username", user.getUsername());
        claims.put("email", user.getEmail());

        var token = Jwts.builder()
                .setClaims(claims)
                .setSubject(user.getId().toString())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();

        return token;
    }

}
