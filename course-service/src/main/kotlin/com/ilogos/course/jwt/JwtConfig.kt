package com.ilogos.course.jwt;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;

@Getter
@Configuration
public class JwtConfig {

    @Value("${jwt.publicPath}")
    private String publicKeyPath;

}
