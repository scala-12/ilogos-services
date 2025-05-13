package com.ilogos.security.user;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.ilogos.security.exception.ExceptionWithStatus;
import com.ilogos.security.jwt.JwtService;
import com.ilogos.security.user.UserController.UpdateUserRequest;
import com.ilogos.security.user.emailHistory.EmailHistory;
import com.ilogos.security.user.emailHistory.EmailHistoryRepository;
import com.ilogos.security.user.model.RoleType;
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

    public Optional<UserWithTokens> generateTokens(String usernameOrEmail, String password) {
        return userRepository.findByEmailOrUsername(usernameOrEmail, usernameOrEmail).map(user -> {
            var tokens = generateTokens(user, Optional.empty());

            user.setLastTokenIssuedAt(jwtService.getTokenInfo(tokens.accessToken), true);
            update(user);

            return new UserWithTokens(user, tokens);
        });
    }

    private TokensData generateTokens(User user, Optional<String> optionalRefreshToken) {
        String accessToken = jwtService.generateToken(user, true);
        String refreshToken = optionalRefreshToken.orElseGet(() -> jwtService.generateToken(user, false));

        return new TokensData(accessToken, refreshToken);
    }

    public Optional<TokensData> refreshUserToken(TokenInfo tokenInfo) {
        User user = userRepository.findById(tokenInfo.getId())
                .orElseThrow(() -> new ExceptionWithStatus(HttpStatus.UNAUTHORIZED));
        if (tokenInfo.isRefresh() && tokenInfo.isValid(user, false)) {
            String username = tokenInfo.getUsername();

            var tokens = generateTokens(user, Optional.of(tokenInfo.getToken()));
            user.setLastTokenIssuedAt(jwtService.getTokenInfo(tokens.accessToken), false);
            update(user);

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

    public Optional<User> updateSelf(TokenInfo tokenInfo, UpdateUserRequest request) {
        Optional<User> user = userRepository.findById(tokenInfo.getId());
        return user.map(e -> {
            request.email().ifPresent(e::setEmail);
            request.password().ifPresent(e::setPassword);
            request.username().ifPresent(e::setUsername);

            return update(e);
        });
    }

}
