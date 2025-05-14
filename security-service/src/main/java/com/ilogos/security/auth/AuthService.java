package com.ilogos.security.auth;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

import com.ilogos.security.exception.ExceptionWithStatus;
import com.ilogos.security.jwt.JwtService;
import com.ilogos.security.user.User;
import com.ilogos.security.user.UserService;
import com.ilogos.security.user.UserService.TokensData;
import com.ilogos.security.utils.TokenInfo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class AuthService {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public record UserWithTokens(User user, TokensData tokens) {
    }

    private TokensData generateTokens(User user, Optional<String> optionalRefreshToken) {
        String accessToken = jwtService.generateToken(user, true);
        String refreshToken = optionalRefreshToken.orElseGet(() -> jwtService.generateToken(user, false));

        return new TokensData(accessToken, refreshToken);
    }

    public Optional<TokensData> authenticate(String usernameOrEmail, String password) {
        log.info("User auth: {}", usernameOrEmail);

        var userWithTokens = userService.assignJwtTokens(usernameOrEmail, password,
                user -> generateTokens(user, Optional.empty()));
        return userWithTokens.map(data -> {
            try {
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(usernameOrEmail, password));
                log.info("Auth success: {}", data.user().getId());

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

    public Optional<TokensData> refreshJwtToken(TokenInfo tokenInfo) {
        var tokens = userService.assignRefreshToken(tokenInfo,
                user -> generateTokens(user, Optional.of(tokenInfo.getToken())));

        return tokens;
    }

}
