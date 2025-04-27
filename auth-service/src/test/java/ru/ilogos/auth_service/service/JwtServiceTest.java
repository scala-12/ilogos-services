package ru.ilogos.auth_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.jsonwebtoken.security.Keys;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        var secretKey = Keys.hmacShaKeyFor("verysecretkeyverysecretkeyverysecretkey".getBytes());

        jwtService = JwtService.create(secretKey, 28800000, 604800000);
        jwtService.init();
    }

    @Test
    void generateAndValidateAccessToken() {
        String username = "testuser";
        String token = jwtService.generateAccessToken(Map.of(), username);

        assertNotNull(token);
        assertEquals(username, jwtService.extractUsername(token));
        assertTrue(jwtService.isTokenValid(token, username));
    }

    @Test
    void tokenShouldExpire() throws InterruptedException {
        String username = "testuser";
        String token = jwtService.generateAccessToken(Map.of(), username);

        assertTrue(jwtService.isTokenValid(token, username));
    }
}
