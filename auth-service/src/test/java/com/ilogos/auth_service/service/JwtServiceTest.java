package com.ilogos.auth_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ilogos.auth_service.auth.JwtService;
import com.ilogos.auth_service.user.RoleType;
import com.ilogos.auth_service.user.User;

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
    void setUp() throws NoSuchAlgorithmException, NoSuchProviderException, IOException {
        SecureRandom random = SecureRandom.getInstance("SHA1PRNG", "SUN");
        random.nextBytes(new byte[] {}); // toss out the first result to ensure it seeds randomly from the system.

        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048, random);
        KeyPair keyPair = keyGen.genKeyPair();
        jwtService = JwtService.create(keyPair, 28800000, 604800000);
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
