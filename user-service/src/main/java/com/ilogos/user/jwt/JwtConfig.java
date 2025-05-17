package com.ilogos.user.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {

    private String secretKeyPath;
    private String publicKeyPath;
    private long accessTokenExpirationMs;
    private long refreshTokenExpirationMs;

    public long getRefreshTokenExpiration() {
        return refreshTokenExpirationMs / 1_000;
    }

}
