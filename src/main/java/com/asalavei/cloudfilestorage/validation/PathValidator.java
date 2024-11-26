package com.asalavei.cloudfilestorage.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class PathValidator implements ConstraintValidator<ValidPath, String> {

    private static final String PATH_PATTERN = "^(/[^/]+)*/?$";
    private static final Pattern PATTERN = Pattern.compile(PATH_PATTERN);

    @Override
    public boolean isValid(final String path, final ConstraintValidatorContext context) {
        if (path == null || path.isEmpty()) {
            return false;
        }

        return PATTERN.matcher(path).matches();
    }
}
