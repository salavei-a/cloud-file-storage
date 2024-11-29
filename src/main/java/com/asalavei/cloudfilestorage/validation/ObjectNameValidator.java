package com.asalavei.cloudfilestorage.validation;

import com.asalavei.cloudfilestorage.validation.constraint.ValidObjectName;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class ObjectNameValidator implements ConstraintValidator<ValidObjectName, String> {

    private static final String PATH_PATTERN = "^[^/]*$";
    private static final Pattern PATTERN = Pattern.compile(PATH_PATTERN);

    @Override
    public boolean isValid(final String name, final ConstraintValidatorContext constraintValidatorContext) {
        if (".".equals(name) || "..".equals(name)) {
            return false;
        }

        return PATTERN.matcher(name).matches();
    }
}
