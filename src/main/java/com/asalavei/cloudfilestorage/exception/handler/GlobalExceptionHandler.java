package com.asalavei.cloudfilestorage.exception.handler;

import com.asalavei.cloudfilestorage.util.HttpUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static com.asalavei.cloudfilestorage.common.Constants.ERROR_404_VIEW;
import static com.asalavei.cloudfilestorage.common.Constants.ERROR_500_VIEW;
import static com.asalavei.cloudfilestorage.common.Constants.MESSAGE_ATTRIBUTE;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final String VALIDATION_FAILED_MESSAGE = "Validation failed for request: {} | Details: {}";

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNoResourceFoundException() {
        return ERROR_404_VIEW;
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleException(Exception e) {
        log.error("Unexpected error occurred", e);
        return ERROR_500_VIEW;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public String handleConstraintViolationException(ConstraintViolationException e, HttpServletRequest request,
                                                     RedirectAttributes redirectAttributes) {
        log.info(VALIDATION_FAILED_MESSAGE, request.getRequestURI(), e.getConstraintViolations());

        String message = e.getConstraintViolations().iterator().next().getMessage();
        redirectAttributes.addFlashAttribute(MESSAGE_ATTRIBUTE, message);

        return HttpUtils.redirectToReferer(request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public String handleMethodArgumentNotValidException(MethodArgumentNotValidException e, HttpServletRequest request,
                                                        RedirectAttributes redirectAttributes) {
        log.info(VALIDATION_FAILED_MESSAGE, request.getRequestURI(), e.getBindingResult().getFieldErrors());

        String message = e.getBindingResult().getFieldErrors().getFirst().getDefaultMessage();
        redirectAttributes.addFlashAttribute(MESSAGE_ATTRIBUTE, message);

        return HttpUtils.redirectToReferer(request);
    }
}
