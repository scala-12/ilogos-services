package com.ilogos.auth_service.controller;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ilogos.auth_service.exceptions.ExceptionWithStatus;
import com.ilogos.auth_service.model.RoleType;
import com.ilogos.auth_service.model.TokenInfo;
import com.ilogos.auth_service.model.dto.UserDTO;
import com.ilogos.auth_service.model.response.SuccessResponse;
import com.ilogos.auth_service.service.JwtService;
import com.ilogos.auth_service.service.UserService;
import com.ilogos.auth_service.service.UserService.TokensData;
import com.ilogos.auth_service.validation.annotation.ValidTimezone;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@AllArgsConstructor
public class AuthController {

    private record RegisterRequest(
            @Size(min = 3, max = 64) String username,
            @NotBlank @Email String email,
            @Size(min = 6, max = 64) String password,
            Optional<Boolean> isActive,
            @NotEmpty List<RoleType> roles,
            @ValidTimezone String timezone) {
    }

    private record LoginRequest(
            String username,
            @Email String email,
            @Size(min = 3, max = 64) String password) {
    }

    private final UserService userService;
    private final JwtService jwtService;

    @PostMapping("/register")
    public ResponseEntity<SuccessResponse<UserDTO>> register(@Valid @RequestBody RegisterRequest req) {
        var roles = req.roles.stream()
                .filter(e -> !e.equals(RoleType.ROLE_ADMIN))
                .collect(Collectors.toSet());
        if (roles.size() == 0 && req.roles.contains(RoleType.ROLE_ADMIN)) {
            throw new ExceptionWithStatus(HttpStatus.BAD_REQUEST, "Administrator registration is denied");
        }
        var user = userService.create(
                req.username,
                req.email,
                req.password,
                req.isActive.orElseGet(() -> true),
                roles,
                req.timezone);

        return SuccessResponse.response(HttpStatus.CREATED, UserDTO.from(user));
    }

    @PostMapping("/login")
    public ResponseEntity<SuccessResponse<TokensData>> login(@Valid @RequestBody LoginRequest req)
            throws NotFoundException {
        if (req.email == null && req.username == null) {
            log.error("login: Username not provided");
            throw new ExceptionWithStatus(HttpStatus.UNAUTHORIZED, "Username not provided");
        }

        String username = req.email == null
                ? req.username
                : req.email;

        var tokens = userService.authenticate(username, req.password)
                .orElseThrow(() -> new ExceptionWithStatus(HttpStatus.UNAUTHORIZED,
                        "Unable to log in with the provided data"));

        return SuccessResponse.response(tokens);
    }

    @PostMapping("/refresh")
    public ResponseEntity<SuccessResponse<TokensData>> refreshToken(
            @RequestHeader("Authorization") String authHeader) {
        TokenInfo tokenInfo;
        try {
            tokenInfo = jwtService.extractTokenInfoFromHeader(authHeader);
        } catch (ExpiredJwtException ex) {
            throw new ExceptionWithStatus(HttpStatus.UNAUTHORIZED, ex);
        }
        var tokens = userService.refreshUserToken(tokenInfo)
                .orElseThrow(() -> new ExceptionWithStatus(HttpStatus.UNAUTHORIZED,
                        "JWT refresh failed"));

        return SuccessResponse.response(tokens);
    }

}
