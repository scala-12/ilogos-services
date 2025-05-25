package com.ilogos.user.user;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.ilogos.shared.model.TokenInfo;
import com.ilogos.user.exception.ExceptionWithStatus;
import com.ilogos.user.jwt.JwtService;
import com.ilogos.user.user.UserController.UpdateUserRequest;
import com.ilogos.user.user.emailHistory.EmailHistory;
import com.ilogos.user.user.emailHistory.EmailHistoryRepository;
import com.ilogos.user.user.model.RoleType;
import com.ilogos.user.user.usernameHistory.UsernameHistory;
import com.ilogos.user.user.usernameHistory.UsernameHistoryRepository;

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

    public record UserWithTokens(User user, TokensData tokens) {
    }

    public Optional<UserWithTokens> assignJwtTokens(
            String usernameOrEmail,
            String password,
            Function<User, TokensData> generator) {
        return userRepository.<User>findByEmailOrUsername(usernameOrEmail, usernameOrEmail, User.class).map(user -> {
            var tokens = generator.apply(user);

            user.setLastTokenIssuedAt(jwtService.getTokenInfo(tokens.accessToken), true);
            update(user);

            return new UserWithTokens(user, tokens);
        });
    }

    public Optional<TokensData> assignRefreshToken(TokenInfo tokenInfo, Function<User, TokensData> generator) {
        User user = userRepository.findById(UUID.fromString(tokenInfo.getSubject()))
                .orElseThrow(() -> new ExceptionWithStatus(HttpStatus.UNAUTHORIZED));
        if (tokenInfo.isRefreshToken() && user.getUsername().equals(tokenInfo.getUsername())) {
            String username = tokenInfo.getUsername();

            var tokens = generator.apply(user);
            user.setLastTokenIssuedAt(jwtService.getTokenInfo(tokens.accessToken), false);
            update(user);

            log.info("Token refresh success: {}", username);

            return Optional.of(tokens);
        }

        log.info("Token refresh error: {}", user.getUsername());

        return Optional.empty();
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

    public Optional<User> updateSelf(TokenInfo tokenInfo, UpdateUserRequest request) {
        Optional<User> user = userRepository.findById(
                UUID.fromString(tokenInfo.getSubject()));
        return user.map(e -> {
            request.email().ifPresent(e::setEmail);
            request.password().ifPresent(e::setPassword);
            request.username().ifPresent(e::setUsername);

            return update(e);
        });
    }

}
