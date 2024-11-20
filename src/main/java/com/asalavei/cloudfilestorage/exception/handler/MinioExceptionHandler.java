package com.asalavei.cloudfilestorage.exception.handler;

import com.asalavei.cloudfilestorage.exception.MinioOperationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import static com.asalavei.cloudfilestorage.common.Constants.ERROR_500_VIEW;

@Slf4j
@ControllerAdvice
@Order(1)
public class MinioExceptionHandler {

    @ExceptionHandler(MinioOperationException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleMinioOperationException(MinioOperationException e) {
        log.error("Minio operation failed: {}", e.getMessage(), e);
        return ERROR_500_VIEW;
    }
}
