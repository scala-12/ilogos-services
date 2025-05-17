package com.ilogos.security.user.jwt;

import java.util.UUID;

import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CustomJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final JwtUserDetailsService userDetailsService;

    @Override
    public AbstractAuthenticationToken convert(@NonNull Jwt jwt) {
        String id = jwt.getSubject();
        if (id == null) {
            return null;
        }

        UserDetails userDetails = userDetailsService.loadUserById(UUID.fromString(id));

        return new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities());
    }
}
