package com.ilogos.auth_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ilogos.auth_service.entity.User;
import com.ilogos.auth_service.model.RoleType;

class JwtServiceTest {

    private JwtService jwtService;

    private static User createUser() {
        try {
            Method method = User.UserBuilder.class.getDeclaredMethod("id", UUID.class);
            method.setAccessible(true);
            var builder = User.builder().username("testuser").role(RoleType.ROLE_ADMIN);

            method.invoke(builder, UUID.randomUUID());

            return builder.build();
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    @BeforeEach
    void setUp() {
        jwtService = JwtService.create("verysecretkeyverysecretkeyverysecretkey", 28800000, 604800000);
        jwtService.init();
    }

    @Test
    void generateAndValidateAccessToken() {
        var user = createUser();
        String token = jwtService.generateToken(user, true);

        assertNotNull(token);

        var tokenInfo = jwtService.getTokenInfo(token);
        assertEquals(user.getUsername(), tokenInfo.getUsername());
        assertTrue(tokenInfo.isValid(user, false));
    }

    @Test
    void tokenShouldExpire() throws InterruptedException {
        var user = createUser();
        String token = jwtService.generateToken(user, true);

        assertTrue(jwtService.getTokenInfo(token).isValid(user, false));
    }
}
