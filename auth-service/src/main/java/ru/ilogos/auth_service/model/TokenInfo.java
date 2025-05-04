package ru.ilogos.auth_service.model;

import java.security.Key;
import java.util.Date;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

public class TokenInfo {
    @Getter
    private Claims claims;

    public TokenInfo(Claims claims) {
        this.claims = claims;
    }

    public TokenInfo(String token, Key secretKey) {
        this.claims = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String getUsername() {
        return claims.get("username", String.class);
    }

    public boolean isExpired() {
        return claims.getExpiration().before(new Date());
    }

    public boolean isValid(@NotBlank String username) {
        return username.equals(getUsername()) && !isExpired();
    }

    public static String getUsername(String token, Key secretKey) {
        return new TokenInfo(token, secretKey).getUsername();
    }
}