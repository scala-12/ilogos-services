package ru.ilogos.auth_service.service;

import java.security.Key;
import java.util.Date;
import java.util.Map;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.ilogos.auth_service.config.JwtConfig;

@Slf4j
@RequiredArgsConstructor
@Service
public class JwtService {

    private final JwtConfig jwtConfig;

    private Key secretKey;
    private long accessTokenExpiration;
    private long refreshTokenExpiration;

    @Profile("test")
    public static JwtService create(Key secretKey, long accessTokenExpiration, long refreshTokenExpiration) {
        var self = new JwtService(null);
        self.secretKey = secretKey;
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

    public String extractUsername(String token) {
        return getAllClaims(token).getSubject();
    }

    private Claims getAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String generateAccessToken(Map<String, Object> extraClaims, String username) {
        return generateToken(extraClaims, username, accessTokenExpiration);
    }

    public String generateRefreshToken(String username) {
        return generateToken(Map.of(), username, refreshTokenExpiration);
    }

    private String generateToken(Map<String, Object> extraClaims, String username, long expirationMs) {
        log.debug("Token generation: {}", username);

        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(username)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean isTokenValid(String token, String username) {
        log.debug("Check token: {}", username);

        final String extractedUsername = extractUsername(token);
        return (extractedUsername.equals(username)) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return getAllClaims(token).getExpiration();
    }
}
