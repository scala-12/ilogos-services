package com.ilogos.security.user;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

import com.ilogos.security.auth.AuthService;
import com.ilogos.security.exception.ExceptionWithStatus;
import com.ilogos.security.user.UserController.UpdateUserRequest;
import com.ilogos.security.user.common.RoleType;
import com.ilogos.security.user.emailHistory.EmailHistory;
import com.ilogos.security.user.emailHistory.EmailHistoryRepository;
import com.ilogos.security.user.usernameHistory.UsernameHistory;
import com.ilogos.security.user.usernameHistory.UsernameHistoryRepository;
import com.ilogos.security.utils.TokenInfo;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final AuthService authService;

    private final UserRepository userRepository;
    private final UsernameHistoryRepository usernameHistoryRepository;
    private final EmailHistoryRepository emailHistoryRepository;
    private final AuthenticationManager authenticationManager;

    public record TokensData(String accessToken, String refreshToken) {
    }

    @Transactional
    public User create(String username, String email, String password, boolean isActive,
            Collection<RoleType> roles,
            String timezone) {
        User user = User.builder().username(username).email(email).password(password).roles(roles)
                .isActive(isActive)
                .timezone(timezone).build();
        user = userRepository.save(user);

        usernameHistoryRepository.findCurrentByUser(user).ifPresent(history -> {
            history.setEndAt(Instant.now());
            usernameHistoryRepository.save(history);
        });
        usernameHistoryRepository.save(new UsernameHistory(user));

        emailHistoryRepository.findCurrentByUser(user).ifPresent(history -> {
            history.setEndAt(Instant.now());
            emailHistoryRepository.save(history);
        });
        emailHistoryRepository.save(new EmailHistory(user));

        return user;
    }

    public Optional<TokensData> authenticate(String usernameOrEmail, String password) {
        log.info("User auth: {}", usernameOrEmail);
        return userRepository.findByEmailOrUsername(usernameOrEmail, usernameOrEmail).map(user -> {
            try {
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(usernameOrEmail, password));

                var tokens = generateTokens(user, Optional.empty());
                user.setLastTokenIssuedAt(authService.getTokenInfo(tokens.accessToken), true);

                log.info("Auth success: {}", usernameOrEmail);

                return tokens;
            } catch (DisabledException ex) {
                log.info("Auth error (disabled): {}", usernameOrEmail);
                throw new ExceptionWithStatus(HttpStatus.UNAUTHORIZED, ex);
            } catch (AuthenticationException ex) {
                log.info("Auth error: {}", usernameOrEmail);
                updateFailedAttempts(user);
                throw new ExceptionWithStatus(HttpStatus.UNAUTHORIZED, ex);
            }
        });
    }

    private TokensData generateTokens(User user, Optional<String> optionalRefreshToken) {
        String accessToken = authService.generateToken(user, true);
        String refreshToken = optionalRefreshToken.orElseGet(() -> authService.generateToken(user, false));

        return new TokensData(accessToken, refreshToken);
    }

    public Optional<TokensData> refreshUserToken(TokenInfo tokenInfo) {
        User user = userRepository.findById(tokenInfo.getId())
                .orElseThrow(() -> new ExceptionWithStatus(HttpStatus.UNAUTHORIZED));
        if (tokenInfo.isRefresh() && tokenInfo.isValid(user, false)) {
            String username = tokenInfo.getUsername();

            var tokens = generateTokens(user, Optional.of(tokenInfo.getToken()));
            user.setLastTokenIssuedAt(authService.getTokenInfo(tokens.accessToken), false);

            log.info("Token refresh success: {}", username);

            return Optional.of(tokens);
        } else {
            log.info("Token refresh error: {}", user.getUsername());

            return Optional.empty();
        }
    }

    @Transactional
    public User update(User user) {
        user.preUpdate();
        user = userRepository.save(user);

        if (user.hasChangedUsername()) {
            usernameHistoryRepository.save(new UsernameHistory(user));
        }

        if (user.hasChangedEmail()) {
            emailHistoryRepository.save(new EmailHistory(user));
        }

        return user;
    }

    public User updateFailedAttempts(User user) {
        user.incrementAttempts();
        return update(user);
    }

    public Optional<User> updateByAuth(TokenInfo tokenInfo, UpdateUserRequest request) {
        Optional<User> user = userRepository.findById(tokenInfo.getId());
        return user.map(e -> {
            request.email().ifPresent(e::setEmail);
            request.password().ifPresent(e::setPassword);
            request.username().ifPresent(e::setUsername);

            return update(e);
        });
    }

}
