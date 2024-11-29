package com.asalavei.cloudfilestorage.validation.constraint;

import com.asalavei.cloudfilestorage.validation.ObjectPathValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({FIELD, PARAMETER})
@Retention(RUNTIME)
@Constraint(validatedBy = ObjectPathValidator.class)
public @interface ValidObjectPath {
    String message() default "Could not execute action";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
