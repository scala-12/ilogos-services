package com.ilogos.security.auth;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

import com.ilogos.security.exception.ExceptionWithStatus;
import com.ilogos.security.user.UserService;
import com.ilogos.security.user.UserService.TokensData;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class AuthService {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;

    public Optional<TokensData> authenticate(String usernameOrEmail, String password) {
        log.info("User auth: {}", usernameOrEmail);

        var userWithTokens = userService.generateTokens(usernameOrEmail, password);
        return userWithTokens.map(data -> {
            try {
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(usernameOrEmail, password));
                log.info("Auth success: {}", usernameOrEmail);

                return data.tokens();
            } catch (DisabledException ex) {
                log.info("Auth error (disabled): {}", usernameOrEmail);
                throw new ExceptionWithStatus(HttpStatus.UNAUTHORIZED, ex);
            } catch (AuthenticationException ex) {
                log.info("Auth error (fail): {}", usernameOrEmail);
                userService.updateFailedAttempts(data.user());
                throw new ExceptionWithStatus(HttpStatus.UNAUTHORIZED, ex);
            }
        });
    }

}
