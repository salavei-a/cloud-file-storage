package com.asalavei.cloudfilestorage.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PathValidator implements ConstraintValidator<ValidPath, String> {

    private static final String PATH_PATTERN = "^(/[^/]+)*/?$";

    @Override
    public boolean isValid(String path, ConstraintValidatorContext context) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        return path.matches(PATH_PATTERN);
    }
}
