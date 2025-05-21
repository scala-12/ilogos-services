package com.ilogos.user.common;

import java.security.PublicKey;
import java.util.Date;
import java.util.UUID;

import org.springframework.security.oauth2.jwt.Jwt;

import com.ilogos.user.user.model.IUserBase;

import io.jsonwebtoken.Jwts;
import lombok.Getter;

@Getter
public class TokenInfo implements IUserBase {
    public final static String USERNAME_CLAIM = "username";
    public final static String EMAIL_CLAIM = "email";
    public final static String TYPE_CLAIM = "type";

    public final static String ACCESS_TYPE = "access";
    public final static String REFRESH_TYPE = "refresh";

    private final String token;
    private final Date expiration;
    private final String username;
    private final UUID id;
    private final String email;
    private final Date issuedAt;
    private final String type;

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

    public boolean isValid(IUserBase user) {
        return user != null && user.getUsername().equals(username) && !isExpired();
    }

    public static boolean isAccessType(String type) {
        return ACCESS_TYPE.equals(type);
    }

    public boolean isAccessToken() {
        return isAccessType(type);
    }

    public static boolean isRefreshType(String type) {
        return REFRESH_TYPE.equals(type);
    }

    public boolean isRefreshToken() {
        return isRefreshType(type);
    }
}