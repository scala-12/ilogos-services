package com.ilogos.user.response;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public abstract class AbstractResponse {
    private LocalDateTime timestamp = LocalDateTime.now();
}
