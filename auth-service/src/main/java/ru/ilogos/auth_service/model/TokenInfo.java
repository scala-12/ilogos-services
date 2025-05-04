package ru.ilogos.auth_service.model;

import java.security.Key;
import java.util.Date;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.Getter;
import ru.ilogos.auth_service.entity.User;

public class TokenInfo {
    @Getter
    private Claims claims;

    public enum Type {
        ACCESS,
        REFRESH,
        UNDEFINED
    }

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

    public boolean isValid(User user, boolean checkIat) {
        return user != null && user.getUsername().equals(getUsername()) && !isExpired()
                && (!checkIat || !getIssuedAt().toInstant().isBefore(user.getLastTokenIssuedAt()));
    }

    public boolean isValid(User user) {
        return isValid(user, true);
    }

    public Type getType() {
        String type = claims.get("type", String.class);
        if (type == null) {
            return Type.UNDEFINED;
        }

        return switch (type) {
            case "access" -> Type.ACCESS;
            case "refresh" -> Type.REFRESH;
            default -> Type.UNDEFINED;
        };
    }

    public boolean isAccess() {
        return Type.ACCESS.equals(getType());
    }

    public Date getIssuedAt() {
        return claims.getIssuedAt();
    }

    public static String getUsername(String token, Key secretKey) {
        return new TokenInfo(token, secretKey).getUsername();
    }
}