package com.ilogos.auth_service.response;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ErrorResponse extends AbstractResponse {
    @Schema(description = "Errors list", example = "[\"Invalid credentials\"]")
    private List<String> errors = null;

    public static ResponseEntity<ErrorResponse> response(HttpStatus status, List<String> errors) {
        return ResponseEntity.status(status).body(new ErrorResponse(errors));
    }

    public static ResponseEntity<ErrorResponse> response(HttpStatus status, String error) {
        return ErrorResponse.response(status, error != null ? List.of(error) : List.of());
    }

    public static ResponseEntity<ErrorResponse> response(HttpStatus status, Exception ex) {
        return ErrorResponse.response(status, ex.getMessage());
    }
}
