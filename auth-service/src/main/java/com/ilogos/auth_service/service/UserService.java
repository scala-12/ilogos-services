package com.ilogos.auth_service.service;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

import com.ilogos.auth_service.controller.UserController.UpdateUserRequest;
import com.ilogos.auth_service.entity.EmailHistory;
import com.ilogos.auth_service.entity.User;
import com.ilogos.auth_service.entity.UsernameHistory;
import com.ilogos.auth_service.exceptions.ExceptionWithStatus;
import com.ilogos.auth_service.model.RoleType;
import com.ilogos.auth_service.model.TokenInfo;
import com.ilogos.auth_service.repository.EmailHistoryRepository;
import com.ilogos.auth_service.repository.UserRepository;
import com.ilogos.auth_service.repository.UsernameHistoryRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final JwtService jwtService;

    private final UserRepository userRepository;
    private final UsernameHistoryRepository usernameHistoryRepository;
    private final EmailHistoryRepository emailHistoryRepository;
    private final AuthenticationManager authenticationManager;

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

    public Optional<String[]> authenticate(String usernameOrEmail, String password) {
        log.info("User auth: {}", usernameOrEmail);
        return userRepository.findByEmailIgnoreCaseOrUsernameIgnoreCase(usernameOrEmail, usernameOrEmail).map(user -> {
            try {
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(usernameOrEmail, password));

                var tokens = generateTokens(user, Optional.empty());
                user.setLastTokenIssuedAt(jwtService.getTokenInfo(tokens[0]), true);

                log.info("Auth success: {}", usernameOrEmail);

                return tokens;
            } catch (DisabledException ex) {
                log.info("Auth error: {} disabled", usernameOrEmail);
                throw new ExceptionWithStatus(HttpStatus.FORBIDDEN, ex);
            } catch (AuthenticationException ex) {
                log.info("Auth error: {}", usernameOrEmail);
                updateFailedAttempts(user);
                throw new ExceptionWithStatus(HttpStatus.UNAUTHORIZED, ex);
            }
        });
    }

    private String[] generateTokens(User user, Optional<String> optionalRefreshToken) {
        String accessToken = jwtService.generateToken(user, true);
        String refreshToken = optionalRefreshToken.orElseGet(() -> jwtService.generateToken(user, false));

        return new String[] { accessToken, refreshToken };
    }

    public Optional<String[]> refreshUserToken(TokenInfo tokenInfo) {
        User user = userRepository.findById(tokenInfo.getId())
                .orElseThrow(() -> new ExceptionWithStatus(HttpStatus.UNAUTHORIZED));
        if (tokenInfo.isValid(user, false)) {
            String username = tokenInfo.getUsername();

            String[] tokens = generateTokens(user, Optional.of(tokenInfo.getToken()));
            user.setLastTokenIssuedAt(jwtService.getTokenInfo(tokens[0]), false);

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

    public Optional<User> updateByAuth(String authToken, UpdateUserRequest request) {
        var tokenInfo = jwtService.extractTokenInfoFromHeader(authToken);
        Optional<User> user = userRepository.findById(tokenInfo.getId());
        return user.map(e -> {
            request.email().ifPresent(e::setEmail);
            request.password().ifPresent(e::setPassword);
            request.username().ifPresent(e::setUsername);

            return update(e);
        });
    }

}
