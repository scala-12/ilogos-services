package com.ilogos.user.user.jwt;

import java.util.Optional;
import java.util.UUID;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.ilogos.user.user.User;
import com.ilogos.user.user.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    private UserDetails getUserDetails(Optional<User> user) {
        return new JwtUserDetails(user.orElseThrow(() -> new UsernameNotFoundException("User not found")));
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var user = userRepository.<User>findByEmailOrUsername(username, username, User.class);

        return getUserDetails(user);
    }

    public UserDetails loadUserById(UUID id) {
        var user = userRepository.findById(id);

        return getUserDetails(user);
    }
}
