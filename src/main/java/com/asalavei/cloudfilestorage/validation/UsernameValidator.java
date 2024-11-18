package com.asalavei.cloudfilestorage.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UsernameValidator implements ConstraintValidator<ValidUsername, String> {

    private static final String USERNAME_PATTERN = "^\\s*[_.@A-Za-z0-9-]*\\s*$";
    private static final Pattern PATTERN = Pattern.compile(USERNAME_PATTERN);

    @Override
    public boolean isValid(final String username, final ConstraintValidatorContext constraintValidatorContext) {
        return validateUsername(username);
    }

    private boolean validateUsername(final String username) {
        Matcher matcher = PATTERN.matcher(username);
        return matcher.matches();
    }
}
