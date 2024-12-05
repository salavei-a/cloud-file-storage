package com.asalavei.cloudfilestorage.exception;

import com.asalavei.cloudfilestorage.util.HttpUtil;
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

import static com.asalavei.cloudfilestorage.util.Constants.ERROR_404_VIEW;
import static com.asalavei.cloudfilestorage.util.Constants.ERROR_500_VIEW;
import static com.asalavei.cloudfilestorage.util.Constants.MESSAGE_ATTRIBUTE;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNoResourceFoundException(HttpServletRequest request) {
        log.warn("Resource not found for request [{}]", request.getRequestURI());
        return ERROR_404_VIEW;
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleException(Exception e, HttpServletRequest request) {
        log.error("Unexpected error occurred for request [{}]", request.getRequestURI(), e);
        return ERROR_500_VIEW;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public String handleConstraintViolationException(ConstraintViolationException e, HttpServletRequest request,
                                                     RedirectAttributes redirectAttributes) {
        log.info("ConstraintViolationException: Validation failed for request [{}]. Violations: {}",
                request.getRequestURI(), e.getConstraintViolations());

        String message = e.getConstraintViolations().iterator().next().getMessage();
        redirectAttributes.addFlashAttribute(MESSAGE_ATTRIBUTE, message);

        return HttpUtil.redirectToReferer(request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public String handleMethodArgumentNotValidException(MethodArgumentNotValidException e, HttpServletRequest request,
                                                        RedirectAttributes redirectAttributes) {
        log.info("MethodArgumentNotValidException: Invalid arguments in request [{}]. Errors: {}",
                request.getRequestURI(), e.getBindingResult().getFieldErrors());

        String message = e.getBindingResult().getFieldErrors().getFirst().getDefaultMessage();
        redirectAttributes.addFlashAttribute(MESSAGE_ATTRIBUTE, message);

        return HttpUtil.redirectToReferer(request);
    }
}
