package ru.ilogos.auth_service.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.ConstraintViolationException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.ilogos.auth_service.entity.User;
import ru.ilogos.auth_service.exceptions.ExceptionWithStatus;
import ru.ilogos.auth_service.model.RoleType;
import ru.ilogos.auth_service.model.response.ErrorResponse;
import ru.ilogos.auth_service.model.response.SuccessResponse;
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

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<SuccessResponse<User>> register(@RequestBody RegistrationRequest req) {
        User user = userService.create(
                req.username(),
                req.email(),
                req.password(),
                req.isActive().orElseGet(() -> true),
                req.roles(),
                req.timezone());
        return SuccessResponse.response(HttpStatus.CREATED, user);
    }

    @PostMapping("/login")
    public ResponseEntity<SuccessResponse<Boolean>> login(@RequestBody AuthRequest req) throws NotFoundException {
        var user = userService.findUser(req.username(), req.email());

        return SuccessResponse.response(user.orElseThrow(() -> new NotFoundException()).equalsPassword(req.password()));
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

}
