package com.ilogos.course.exception;

import org.springframework.http.HttpStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ExceptionWithStatus extends RuntimeException {
    final private HttpStatus status;

    public ExceptionWithStatus(HttpStatus status, Exception ex) {
        super(ex.getMessage());
        this.status = status;
    }

    public ExceptionWithStatus(HttpStatus status, String msg) {
        super(msg);
        this.status = status;
    }
}
