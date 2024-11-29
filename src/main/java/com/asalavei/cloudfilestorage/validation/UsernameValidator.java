package com.asalavei.cloudfilestorage.validation;

import com.asalavei.cloudfilestorage.validation.constraint.ValidUsername;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class UsernameValidator implements ConstraintValidator<ValidUsername, String> {

    private static final String USERNAME_PATTERN = "^\\s*[_.@A-Za-z0-9-]*\\s*$";
    private static final Pattern PATTERN = Pattern.compile(USERNAME_PATTERN);

    @Override
    public boolean isValid(final String username, final ConstraintValidatorContext constraintValidatorContext) {
        return PATTERN.matcher(username).matches();
    }
}
