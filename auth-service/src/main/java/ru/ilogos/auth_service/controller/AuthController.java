package ru.ilogos.auth_service.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.jsonwebtoken.security.SignatureException;
import jakarta.validation.ConstraintViolationException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.ilogos.auth_service.entity.User;
import ru.ilogos.auth_service.exceptions.ExceptionWithStatus;
import ru.ilogos.auth_service.model.RoleType;
import ru.ilogos.auth_service.model.response.ErrorResponse;
import ru.ilogos.auth_service.model.response.SuccessResponse;
import ru.ilogos.auth_service.service.JwtService;
import ru.ilogos.auth_service.service.UserService;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@AllArgsConstructor
public class AuthController {

    private record RegistrationRequest(String username, String email, String password, Optional<Boolean> isActive,
            List<RoleType> roles, String timezone) {
    }

    private record AuthRequest(Optional<String> username, Optional<String> email, String password) {
    }

    private record RefreshAuthRequest(String username, String refreshToken) {
    }

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @PostMapping("/register")
    public ResponseEntity<SuccessResponse<User>> register(@RequestBody RegistrationRequest req) {
        User user = userService.create(
                req.username,
                req.email,
                req.password,
                req.isActive.orElseGet(() -> true),
                req.roles,
                req.timezone);
        return SuccessResponse.response(HttpStatus.CREATED, user);
    }

    private ResponseEntity<SuccessResponse<Map<?, ?>>> getJwtResponse(
            User user, Optional<String> optionalRefreshToken) {
        String accessToken = jwtService.generateAccessToken(
                Map.of("roles", user.getRoles().stream().map(RoleType::name).toList()),
                user.getUsername());
        String refreshToken = optionalRefreshToken.orElseGet(() -> jwtService.generateRefreshToken(user.getUsername()));

        return SuccessResponse.response(Map.of("accessToken", accessToken, "refreshToken", refreshToken));
    }

    @PostMapping("/login")
    public ResponseEntity<SuccessResponse<Map<?, ?>>> login(@RequestBody AuthRequest req) throws NotFoundException {
        String username = req.email.orElse(req.username.orElse(null));
        if (username == null) {
            log.error("login: Username not provided");
            throw new RuntimeException("Username not provided");
        }

        log.info("User auth: {}", username);
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, req.password));

            User user = userService.findUser(username)
                    .orElseThrow(() -> new ExceptionWithStatus(HttpStatus.NON_AUTHORITATIVE_INFORMATION));

            log.info("Auth success: {}", username);

            return getJwtResponse(user, Optional.empty());
        } catch (AuthenticationException ex) {
            log.info("Auth error: {}", username);
            throw new ExceptionWithStatus(HttpStatus.NON_AUTHORITATIVE_INFORMATION, ex);
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<SuccessResponse<Map<?, ?>>> refreshToken(@RequestBody RefreshAuthRequest req) {
        if (jwtService.isTokenValid(req.refreshToken, req.username)) {
            String username = jwtService.extractUsername(req.refreshToken);

            log.info("Refresh token: {}", username);

            User user = userService.findUser(username)
                    .orElseThrow(() -> new ExceptionWithStatus(HttpStatus.NON_AUTHORITATIVE_INFORMATION));

            log.info("Token refresh success: {}", username);

            return getJwtResponse(user, Optional.of(req.refreshToken));
        } else {
            log.info("Token refresh error: {}", req.username);

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @ExceptionHandler(ExceptionWithStatus.class)
    public ResponseEntity<ErrorResponse> handleExceptionWithStatus(ExceptionWithStatus ex) {
        return ErrorResponse.response(ex.getStatus(), ex.getMessage());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(ConstraintViolationException ex) {
        List<String> errors = ex.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .toList();

        return ErrorResponse.response(HttpStatus.BAD_REQUEST, errors);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        return ErrorResponse.response(HttpStatus.BAD_REQUEST, ex);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex) {
        return ErrorResponse.response(HttpStatus.INTERNAL_SERVER_ERROR, ex);
    }

    @ExceptionHandler(SignatureException.class)
    public ResponseEntity<ErrorResponse> handleException(SignatureException ex) {
        return ErrorResponse.response(HttpStatus.BAD_REQUEST, ex);
    }

}
