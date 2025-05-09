package com.ilogos.auth_service.controller;

import java.util.List;
import java.util.Map;
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
import com.ilogos.auth_service.model.dto.UserDTO;
import com.ilogos.auth_service.model.response.SuccessResponse;
import com.ilogos.auth_service.service.JwtService;
import com.ilogos.auth_service.service.UserService;
import com.ilogos.auth_service.validation.annotation.ValidTimezone;

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

    private record RegistrationRequest(
            @Size(min = 3, max = 64) String username,
            @NotBlank @Email String email,
            @Size(min = 6, max = 64) String password,
            Optional<Boolean> isActive,
            @NotEmpty List<RoleType> roles,
            @ValidTimezone String timezone) {
    }

    private record AuthRequest(Optional<String> username, Optional<String> email, String password) {
    }

    private final UserService userService;
    private final JwtService jwtService;

    @PostMapping("/register")
    public ResponseEntity<SuccessResponse<UserDTO>> register(@Valid @RequestBody RegistrationRequest req) {
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

    private static ResponseEntity<SuccessResponse<Map<?, ?>>> getJwtResponse(String[] tokens) {
        return SuccessResponse.response(Map.of("accessToken", tokens[0], "refreshToken", tokens[1]));
    }

    @PostMapping("/login")
    public ResponseEntity<SuccessResponse<Map<?, ?>>> login(@RequestBody AuthRequest req) throws NotFoundException {
        if (req.email.isEmpty() && req.username.isEmpty()) {
            log.error("login: Username not provided");
            throw new RuntimeException("Username not provided");
        }

        String username = req.email.orElse(req.username.get());

        return userService.authenticate(username, req.password).map(AuthController::getJwtResponse)
                .orElseThrow(() -> new ExceptionWithStatus(HttpStatus.UNAUTHORIZED, "Unable to log in with the provided data"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<SuccessResponse<Map<?, ?>>> refreshToken(@RequestHeader("Authorization") String authHeader) {
        var tokenInfo = jwtService.extractTokenInfoFromHeader(authHeader);
        var tokens = userService.refreshUserToken(tokenInfo);
        return tokens.map(AuthController::getJwtResponse)
                .orElseThrow(() -> new ExceptionWithStatus(HttpStatus.UNAUTHORIZED, "JWT refresh failed"));
    }

}
