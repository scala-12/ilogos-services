package ru.ilogos.auth_service.service;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.ilogos.auth_service.entity.EmailHistory;
import ru.ilogos.auth_service.entity.User;
import ru.ilogos.auth_service.entity.UsernameHistory;
import ru.ilogos.auth_service.model.RoleType;
import ru.ilogos.auth_service.model.TokenInfo;
import ru.ilogos.auth_service.repository.EmailHistoryRepository;
import ru.ilogos.auth_service.repository.UserRepository;
import ru.ilogos.auth_service.repository.UsernameHistoryRepository;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UsernameHistoryRepository usernameHistoryRepository;
    private final EmailHistoryRepository emailHistoryRepository;

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

    public Optional<User> findUser(Optional<String> username, Optional<String> email) {
        if (!username.isPresent() && !email.isPresent()) {
            throw new IllegalArgumentException("username and email undefined");
        }
        Optional<User> user;
        if (username.isPresent()) {
            user = userRepository.findByUsernameIgnoreCase(username.get());
        } else {
            user = userRepository.findByEmailIgnoreCase(email.get());
        }

        return user;
    }

    public Optional<User> findUser(String usernameOrEmail) {
        return userRepository.findByEmailIgnoreCaseOrUsernameIgnoreCase(usernameOrEmail, usernameOrEmail);
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

    public User updateTokenUsing(User user, TokenInfo info, boolean isLogin) {
        user.setLastTokenIssuedAt(info, isLogin);
        return update(user);
    }

    public User updateFailedAttempts(User user) {
        user.incrementAttempts();
        return update(user);
    }

}
