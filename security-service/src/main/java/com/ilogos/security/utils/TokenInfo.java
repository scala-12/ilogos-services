package com.ilogos.security.utils;

import java.security.PublicKey;
import java.util.Date;
import java.util.UUID;

import org.springframework.security.oauth2.jwt.Jwt;

import com.ilogos.security.user.model.IUser;
import com.ilogos.security.user.model.IUserBase;

import io.jsonwebtoken.Jwts;
import lombok.Getter;

@Getter
public class TokenInfo implements IUserBase {
    public final static String USERNAME_CLAIM = "username";
    public final static String EMAIL_CLAIM = "email";
    public final static String TYPE_CLAIM = "type";

    private final String token;
    private final Date expiration;
    private final String username;
    private final UUID id;
    private final String email;
    private final Date issuedAt;
    private final String type;

    private enum Type {
        ACCESS,
        REFRESH,
        UNDEFINED
    }

    public TokenInfo(String token, PublicKey publicKey) {
        this.token = token;
        var claims = Jwts.parserBuilder()
                .setSigningKey(publicKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
        expiration = claims.getExpiration();
        username = claims.get(USERNAME_CLAIM, String.class);
        id = UUID.fromString(claims.getSubject());
        email = claims.get(EMAIL_CLAIM, String.class);
        issuedAt = claims.getIssuedAt();
        type = claims.get(TYPE_CLAIM, String.class);
    }

    public TokenInfo(Jwt jwt) {
        token = jwt.getTokenValue();

        expiration = Date.from(jwt.getExpiresAt());
        username = jwt.getClaimAsString(USERNAME_CLAIM);
        id = UUID.fromString(jwt.getSubject());
        email = jwt.getClaimAsString(EMAIL_CLAIM);
        issuedAt = Date.from(jwt.getIssuedAt());
        type = jwt.getClaimAsString(TYPE_CLAIM);
    }

    public boolean isExpired() {
        return expiration.before(new Date());
    }

    public boolean isValid(IUser user, boolean checkIat) {
        return user != null && user.getUsername().equals(username) && !isExpired()
                && (!checkIat || !issuedAt.toInstant().isBefore(user.getLastTokenIssuedAt()));
    }

    public static boolean isAccessType(String type) {
        return Type.ACCESS.equals(getType(type));
    }

    public boolean isValid(IUser user) {
        return isValid(user, true);
    }

    private static Type getType(String type) {
        if (type == null) {
            return Type.UNDEFINED;
        }

        return switch (type) {
            case "access" -> Type.ACCESS;
            case "refresh" -> Type.REFRESH;
            default -> Type.UNDEFINED;
        };
    }

    public Type getType() {
        return getType(type);
    }

    public boolean isAccess() {
        return Type.ACCESS.equals(getType());
    }

    public boolean isRefresh() {
        return Type.REFRESH.equals(getType());
    }
}