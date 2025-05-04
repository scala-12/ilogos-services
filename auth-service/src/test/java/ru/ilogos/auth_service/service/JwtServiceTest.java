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
        var user = User.builder().username("testuser").role(RoleType.ROLE_ADMIN).build();
        String token = jwtService.generateToken(user, true);

        assertNotNull(token);

        var tokenInfo = jwtService.getTokenInfo(token);
        assertEquals(user.getUsername(), tokenInfo.getUsername());
        assertTrue(tokenInfo.isValid(user));
    }

    @Test
    void tokenShouldExpire() throws InterruptedException {
        var user = User.builder().username("testuser").role(RoleType.ROLE_ADMIN).build();
        String token = jwtService.generateToken(user, true));

        assertTrue(jwtService.getTokenInfo(token).isValid(user));
    }
}
