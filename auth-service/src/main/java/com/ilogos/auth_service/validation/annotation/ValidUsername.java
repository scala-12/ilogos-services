package com.ilogos.auth_service.validation.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.ilogos.auth_service.validation.UsernameValidator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = UsernameValidator.class)
public @interface ValidUsername {

    String message() default "Username cannot contain '@' or be empty";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
