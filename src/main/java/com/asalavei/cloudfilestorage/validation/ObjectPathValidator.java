package com.asalavei.cloudfilestorage.validation;

import com.asalavei.cloudfilestorage.validation.constraint.ValidObjectPath;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class ObjectPathValidator implements ConstraintValidator<ValidObjectPath, String> {

    private static final String PATH_PATTERN = "^(/[^/]+)*/?$";
    private static final Pattern PATTERN = Pattern.compile(PATH_PATTERN);

    @Override
    public boolean isValid(final String path, final ConstraintValidatorContext context) {
        return PATTERN.matcher(path).matches();
    }
}
