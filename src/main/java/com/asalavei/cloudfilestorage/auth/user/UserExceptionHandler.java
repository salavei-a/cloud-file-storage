package com.asalavei.cloudfilestorage.auth.user;

import com.asalavei.cloudfilestorage.auth.SignUpRequestDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import static com.asalavei.cloudfilestorage.util.Constants.ERROR_MESSAGE_ATTRIBUTE;
import static com.asalavei.cloudfilestorage.util.Constants.SIGNUP_VIEW;
import static com.asalavei.cloudfilestorage.util.Constants.USER_ATTRIBUTE;

@Slf4j
@ControllerAdvice
@Order(1)
public class UserExceptionHandler {

    @ExceptionHandler(UserAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String handleUserAlreadyExistsException(UserAlreadyExistsException e, Model model) {
        model.addAttribute(ERROR_MESSAGE_ATTRIBUTE, e.getMessage());
        model.addAttribute(USER_ATTRIBUTE, SignUpRequestDto.builder().build());
        return SIGNUP_VIEW;
    }
}
