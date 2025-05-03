package ru.ilogos.auth_service.service;

import java.util.Collection;
import java.util.Optional;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.ilogos.auth_service.entity.User;
import ru.ilogos.auth_service.model.RoleType;
import ru.ilogos.auth_service.repository.UserRepository;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User create(String username, String email, String password, boolean isActive,
            Collection<RoleType> roles,
            String timezone) {
        User user = User.builder().username(username).email(email).password(password).roles(roles)
                .isActive(isActive)
                .timezone(timezone).build();
        user = userRepository.save(user);

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
}
