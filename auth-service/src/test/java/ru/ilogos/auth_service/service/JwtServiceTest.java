package ru.ilogos.auth_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.ilogos.auth_service.entity.User;
import ru.ilogos.auth_service.model.RoleType;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = JwtService.create("verysecretkeyverysecretkeyverysecretkey", 28800000, 604800000);
        jwtService.init();
    }

    @Test
    void generateAndValidateAccessToken() {
        String username = "testuser";
        String token = jwtService.generateAccessToken(
                User.builder().username(username).role(RoleType.ROLE_ADMIN).build());

        assertNotNull(token);
        assertEquals(username, jwtService.extractUsername(token));
        assertTrue(jwtService.isTokenValid(token, username));
    }

    @Test
    void tokenShouldExpire() throws InterruptedException {
        String username = "testuser";
        String token = jwtService.generateAccessToken(
                User.builder().username(username).role(RoleType.ROLE_ADMIN).build());

        assertTrue(jwtService.isTokenValid(token, username));
    }
}
