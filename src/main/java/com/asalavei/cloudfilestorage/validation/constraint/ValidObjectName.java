package com.asalavei.cloudfilestorage.validation.constraint;

import com.asalavei.cloudfilestorage.validation.ObjectNameValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({FIELD})
@Retention(RUNTIME)
@Constraint(validatedBy = ObjectNameValidator.class)
public @interface ValidObjectName {
    String message() default "Name cannot contain '/' and cannot be '.' or '..'";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
