package com.ilogos.auth_service.model.response;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SuccessResponse<T> extends AbstractResponse {
    private T data = null;

    public static ResponseEntity<SuccessResponse<?>> response() {
        return ResponseEntity.status(HttpStatus.OK).body(new SuccessResponse<>());
    }

    public static <T> ResponseEntity<SuccessResponse<T>> response(T data) {
        return ResponseEntity.status(HttpStatus.OK).body(new SuccessResponse<>(data));
    }

    public static <T> ResponseEntity<SuccessResponse<T>> response(HttpStatus status, T data) {
        return ResponseEntity.status(status).body(new SuccessResponse<>(data));
    }
}
