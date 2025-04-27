package ru.ilogos.auth_service.validation;

import java.time.ZoneId;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import ru.ilogos.auth_service.validation.annotation.ValidTimezone;

public class TimezoneValidator implements ConstraintValidator<ValidTimezone, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            ZoneId.of(value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
