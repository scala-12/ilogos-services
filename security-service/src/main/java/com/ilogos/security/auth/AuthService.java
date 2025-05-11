package com.ilogos.security.auth;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.ilogos.security.config.JwtConfig;
import com.ilogos.security.exception.ExceptionWithStatus;
import com.ilogos.security.user.User;
import com.ilogos.security.user.common.RoleType;
import com.ilogos.security.utils.TokenInfo;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class AuthService {

    private final JwtConfig jwtConfig;

    private Key secretKey;
    private long accessTokenExpiration;
    private long refreshTokenExpiration;

    @Profile("test")
    public static AuthService create(String secretKey, long accessTokenExpiration, long refreshTokenExpiration) {
        var self = new AuthService(null);
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

    public TokenInfo extractTokenInfoFromHeader(String header) {
        var token = header.startsWith("Bearer ")
                ? Optional.of(header.substring(7))
                : Optional.<String>empty();
        if (token.isEmpty()) {
            log.info("Bearer token not setted");
            throw new ExceptionWithStatus(HttpStatus.UNAUTHORIZED, "Bearer token not setted");
        }
        return new TokenInfo(token.get(), secretKey);
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
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();

        return token;
    }

}
