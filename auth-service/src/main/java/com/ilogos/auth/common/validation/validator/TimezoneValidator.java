package com.ilogos.auth.common.validation.validator;

import java.time.ZoneId;

import com.ilogos.auth.common.validation.annotation.ValidTimezone;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

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
