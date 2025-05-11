package com.ilogos.security.auth;

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

import com.ilogos.security.user.User;
import com.ilogos.security.user.common.RoleType;

class AuthServiceTest {

    private AuthService authService;

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
        authService = AuthService.create(keyPair, 28800000, 604800000);
        authService.init();
    }

    @Test
    void generateAndValidateAccessToken() {
        var user = createUser();
        String token = authService.generateToken(user, true);

        assertNotNull(token);

        var tokenInfo = authService.getTokenInfo(token);
        assertEquals(user.getUsername(), tokenInfo.getUsername());
        assertTrue(tokenInfo.isValid(user, false));
    }

    @Test
    void tokenShouldExpire() throws InterruptedException {
        var user = createUser();
        String token = authService.generateToken(user, true);

        assertTrue(authService.getTokenInfo(token).isValid(user, false));
    }
}
