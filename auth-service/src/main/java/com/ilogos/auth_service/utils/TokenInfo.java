package com.ilogos.auth_service.utils;

import java.security.Key;
import java.util.Date;
import java.util.UUID;

import com.ilogos.auth_service.user.common.UserMinimalView;
import com.ilogos.auth_service.user.common.UserView;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.Getter;

@Getter
public class TokenInfo implements UserMinimalView {
    private final Claims claims;
    private final String token;

    private enum Type {
        ACCESS,
        REFRESH,
        UNDEFINED
    }

    public TokenInfo(String token, Key secretKey) {
        this.token = token;
        this.claims = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean isExpired() {
        return claims.getExpiration().before(new Date());
    }

    public boolean isValid(UserView user, boolean checkIat) {
        return user != null && user.getUsername().equals(getUsername()) && !isExpired()
                && (!checkIat || !getIssuedAt().toInstant().isBefore(user.getLastTokenIssuedAt()));
    }

    public boolean isValid(UserView user) {
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

    public boolean isRefresh() {
        return Type.REFRESH.equals(getType());
    }

    public Date getIssuedAt() {
        return claims.getIssuedAt();
    }

    @Override
    public String getEmail() {
        return claims.get("email", String.class);
    }

    @Override
    public UUID getId() {
        return UUID.fromString(claims.getSubject());
    }

    @Override
    public String getUsername() {
        return claims.get("username", String.class);
    }
}