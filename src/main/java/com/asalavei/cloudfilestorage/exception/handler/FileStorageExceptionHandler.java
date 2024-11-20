package com.asalavei.cloudfilestorage.exception.handler;

import com.asalavei.cloudfilestorage.exception.FileStorageException;
import com.asalavei.cloudfilestorage.exception.MinioOperationException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import static com.asalavei.cloudfilestorage.common.Constants.ERROR_500_VIEW;
import static com.asalavei.cloudfilestorage.common.Constants.HOME_URL;

@Slf4j
@ControllerAdvice
@Order(1)
public class FileStorageExceptionHandler {

    @ExceptionHandler(MinioOperationException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleMinioOperationException(MinioOperationException e) {
        log.error("Minio operation failed: {}", e.getMessage(), e);
        return ERROR_500_VIEW;
    }

    @ExceptionHandler(FileStorageException.class)
    public String handleFileStorageException(FileStorageException e, RedirectAttributes redirectAttributes,
                                             HttpServletRequest request) {
        log.error("File storage operation failed: {}", e.getMessage(), e);
        String referer = request.getHeader("Referer");

        if (referer == null || referer.isBlank()) {
            referer = HOME_URL;
        }

        redirectAttributes.addFlashAttribute("message", e.getMessage());
        return "redirect:" + referer;
    }
}
