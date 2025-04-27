package ru.ilogos.auth_service.service;

import java.security.Key;
import java.util.Date;
import java.util.Map;

import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import ru.ilogos.auth_service.config.JwtConfig;

@RequiredArgsConstructor
@Service
public class JwtService {

    private final JwtConfig jwtConfig;

    private Key secretKey;

    @PostConstruct
    public void init() {
        this.secretKey = Keys.hmacShaKeyFor(jwtConfig.getSecretKey().getBytes());
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
        return generateToken(extraClaims, username, jwtConfig.getAccessTokenExpiration());
    }

    public String generateRefreshToken(String username) {
        return generateToken(Map.of(), username, jwtConfig.getRefreshTokenExpiration());
    }

    private String generateToken(Map<String, Object> extraClaims, String username, long expirationMs) {
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(username)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean isTokenValid(String token, String username) {
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
