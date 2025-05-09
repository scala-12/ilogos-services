package com.ilogos.auth_service.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.ilogos.auth_service.exceptions.ExceptionWithStatus;
import com.ilogos.auth_service.model.response.ErrorResponse;

import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ControllerAdvice
public class ExceptionController {
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
