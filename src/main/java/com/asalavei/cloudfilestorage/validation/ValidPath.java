package com.asalavei.cloudfilestorage.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({FIELD, PARAMETER})
@Retention(RUNTIME)
@Constraint(validatedBy = PathValidator.class)
public @interface ValidPath {
    String message() default "The path format is invalid. Please ensure it follows the correct structure";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
