package com.asalavei.cloudfilestorage.exception.handler;

import com.asalavei.cloudfilestorage.exception.FileStorageException;
import com.asalavei.cloudfilestorage.util.HttpUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@ControllerAdvice
@Order(1)
public class FileStorageExceptionHandler {

    @Value("${spring.servlet.multipart.max-file-size}")
    private String maxFileSize;

    @ExceptionHandler(FileStorageException.class)
    public String handleFileStorageException(FileStorageException e, RedirectAttributes redirectAttributes,
                                             HttpServletRequest request) {
        log.error("File storage operation failed: {}", e.getMessage(), e);
        redirectAttributes.addFlashAttribute("message", e.getMessage());
        return HttpUtils.redirectToReferer(request);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleMaxUploadSizeExceededException(RedirectAttributes redirectAttributes, HttpServletRequest request) {
        redirectAttributes.addFlashAttribute("message", "The uploaded file is too large. Maximum allowed size is " + maxFileSize);
        return HttpUtils.redirectToReferer(request);
    }
}
