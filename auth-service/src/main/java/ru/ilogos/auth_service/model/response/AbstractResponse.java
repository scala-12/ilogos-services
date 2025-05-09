package ru.ilogos.auth_service.model.response;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AbstractResponse {
    private LocalDateTime timestamp = LocalDateTime.now();
}
