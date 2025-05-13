package com.ilogos.security.config.security.filters;

import java.io.IOException;

import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.ilogos.security.auth.AuthService;
import com.ilogos.security.user.UserRepository;
import com.ilogos.security.user.jwt.JwtUserDetailsService;
import com.ilogos.security.utils.TokenInfo;

import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final AuthService authService;
    private final JwtUserDetailsService userDetailsService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        TokenInfo info = null;
        try {
            info = authService.getTokenInfo(authHeader.substring(7));
        } catch (SignatureException ex) {
        } finally {
            if (info == null || !info.isAccess()) {
                filterChain.doFilter(request, response);
                return;
            }
        }
        TokenInfo tokenInfo = info;

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(tokenInfo.getUsername());

            userRepository.findUserByUsername(userDetails.getUsername()).ifPresent((user) -> {
                if (tokenInfo.isValid(user)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities());
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            });
        }

        filterChain.doFilter(request, response);
    }
}
