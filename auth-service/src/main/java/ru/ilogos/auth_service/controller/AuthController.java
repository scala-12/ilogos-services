package ru.ilogos.auth_service.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.ilogos.auth_service.entity.User;
import ru.ilogos.auth_service.exceptions.ExceptionWithStatus;
import ru.ilogos.auth_service.model.RoleType;
import ru.ilogos.auth_service.model.response.ErrorResponse;
import ru.ilogos.auth_service.model.response.SuccessResponse;
import ru.ilogos.auth_service.service.JwtService;
import ru.ilogos.auth_service.service.UserService;
import ru.ilogos.auth_service.validation.annotation.ValidTimezone;

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

    private record RefreshAuthRequest(String username, String refreshToken) {
    }

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @PostMapping("/register")
    public ResponseEntity<SuccessResponse<User>> register(@Valid @RequestBody RegistrationRequest req) {
        var roles = req.roles.stream()
                .filter(e -> !e.equals(RoleType.ROLE_ADMIN))
                .collect(Collectors.toSet());
        if (roles.size() == 0 && req.roles.contains(RoleType.ROLE_ADMIN)) {
            throw new ExceptionWithStatus(HttpStatus.BAD_REQUEST, "Administrator registration is denied");
        }
        User user = userService.create(
                req.username,
                req.email,
                req.password,
                req.isActive.orElseGet(() -> true),
                roles,
                req.timezone);
        return SuccessResponse.response(HttpStatus.CREATED, user);
    }

    private ResponseEntity<SuccessResponse<Map<?, ?>>> getJwtResponse(
            User user, Optional<String> optionalRefreshToken) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = optionalRefreshToken.orElseGet(() -> jwtService.generateRefreshToken(user));

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
        User user = userService.findUser(username)
                .orElseThrow(() -> new ExceptionWithStatus(HttpStatus.UNAUTHORIZED, "Unable to log in with the provided data"));
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, req.password));
            userService.updateLastLogin(user);

            log.info("Auth success: {}", username);

            return getJwtResponse(user, Optional.empty());
        } catch (DisabledException ex) {
            log.info("Auth error: {} disabled", username);
            throw new ExceptionWithStatus(HttpStatus.FORBIDDEN, ex);
        } catch (AuthenticationException ex) {
            log.info("Auth error: {}", username);
            userService.updateFailedAttempts(user);
            throw new ExceptionWithStatus(HttpStatus.UNAUTHORIZED, ex);
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<SuccessResponse<Map<?, ?>>> refreshToken(@RequestBody RefreshAuthRequest req) {
        var tokenInfo = jwtService.getTokenInfo(req.refreshToken);
        if (tokenInfo.isValid(req.username)) {
            String username = tokenInfo.getUsername();

            log.info("Refresh token: {}", username);

            User user = userService.findUser(username)
                    .orElseThrow(() -> new ExceptionWithStatus(HttpStatus.UNAUTHORIZED));

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
        String msg = ex.getMostSpecificCause().getMessage();
        if (msg != null) {
            if (msg.contains("email")) {
                return ErrorResponse.response(HttpStatus.BAD_REQUEST, "Email already used");
            } else if (msg.contains("username")) {
                return ErrorResponse.response(HttpStatus.BAD_REQUEST, "Username already used");
            }
        }

        return ErrorResponse.response(HttpStatus.BAD_REQUEST, ex);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex) {
        return ErrorResponse.response(HttpStatus.INTERNAL_SERVER_ERROR, ex);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        List<String> errors = new ArrayList<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.add("%s: %s".formatted(error.getField(), error.getDefaultMessage())));
        return ErrorResponse.response(HttpStatus.BAD_REQUEST, errors);
    }

    @ExceptionHandler(SignatureException.class)
    public ResponseEntity<ErrorResponse> handleException(SignatureException ex) {
        return ErrorResponse.response(HttpStatus.BAD_REQUEST, ex);
    }

    @ExceptionHandler(MalformedJwtException.class)
    public ResponseEntity<ErrorResponse> handleMalformedJwtException(MalformedJwtException ex) {
        return ErrorResponse.response(HttpStatus.BAD_REQUEST, "Malformed JWT");
    }

}
