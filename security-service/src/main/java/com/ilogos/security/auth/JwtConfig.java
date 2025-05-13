package com.ilogos.security.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;

@Getter
@Configuration
public class JwtConfig {

    @Value("${jwt.secretPath}")
    private String privateKeyPath;

    @Value("${jwt.publicPath}")
    private String publicKeyPath;

    @Value("${jwt.accessTokenExpiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refreshTokenExpiration}")
    private long refreshTokenExpiration;

}
