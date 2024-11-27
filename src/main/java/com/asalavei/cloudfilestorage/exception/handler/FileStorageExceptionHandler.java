package com.asalavei.cloudfilestorage.exception.handler;

import com.asalavei.cloudfilestorage.exception.FileListingException;
import com.asalavei.cloudfilestorage.exception.FileStorageException;
import com.asalavei.cloudfilestorage.exception.ObjectNotFoundException;
import com.asalavei.cloudfilestorage.exception.ObjectExistsException;
import com.asalavei.cloudfilestorage.util.HttpUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import static com.asalavei.cloudfilestorage.common.Constants.ERROR_404_VIEW;
import static com.asalavei.cloudfilestorage.common.Constants.ERROR_500_VIEW;
import static com.asalavei.cloudfilestorage.common.Constants.MESSAGE_ATTRIBUTE;

@Slf4j
@ControllerAdvice
@Order(1)
public class FileStorageExceptionHandler {

    @Value("${spring.servlet.multipart.max-file-size}")
    private String maxFileSize;

    @ExceptionHandler(FileStorageException.class)
    public String handleFileStorageException(FileStorageException e, RedirectAttributes redirectAttributes,
                                             HttpServletRequest request) {
        log.warn("FileStorageException occurred: {}", e.getMessage());
        redirectAttributes.addFlashAttribute(MESSAGE_ATTRIBUTE, e.getMessage());
        return HttpUtils.redirectToReferer(request);
    }

    @ExceptionHandler(FileListingException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleFileListingException(FileListingException e) {
        log.error("FileListingException occurred: {}", e.getMessage());
        return ERROR_500_VIEW;
    }

    @ExceptionHandler(ObjectNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNoObjectFoundException() {
        return ERROR_404_VIEW;
    }

    @ExceptionHandler(ObjectExistsException.class)
    public String handleObjectExistsException(ObjectExistsException e, RedirectAttributes redirectAttributes,
                                              HttpServletRequest request) {
        log.info("ObjectExistsException occurred: {}", e.getMessage());
        redirectAttributes.addFlashAttribute(MESSAGE_ATTRIBUTE, e.getMessage());
        return HttpUtils.redirectToReferer(request);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleMaxUploadSizeExceededException(RedirectAttributes redirectAttributes, HttpServletRequest request) {
        redirectAttributes.addFlashAttribute(MESSAGE_ATTRIBUTE, "The uploaded file is too large. Maximum allowed size is " + maxFileSize);
        return HttpUtils.redirectToReferer(request);
    }
}
