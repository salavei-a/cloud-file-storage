package com.asalavei.cloudfilestorage.storage;

import com.asalavei.cloudfilestorage.storage.exception.FileListingException;
import com.asalavei.cloudfilestorage.storage.exception.FileStorageException;
import com.asalavei.cloudfilestorage.storage.exception.ObjectNotFoundException;
import com.asalavei.cloudfilestorage.storage.exception.ObjectExistsException;
import com.asalavei.cloudfilestorage.util.HttpUtil;
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

import static com.asalavei.cloudfilestorage.util.Constants.ERROR_404_VIEW;
import static com.asalavei.cloudfilestorage.util.Constants.ERROR_500_VIEW;
import static com.asalavei.cloudfilestorage.util.Constants.MESSAGE_ATTRIBUTE;

@Slf4j
@ControllerAdvice
@Order(1)
public class FileStorageExceptionHandler {

    @Value("${spring.servlet.multipart.max-file-size}")
    private String maxFileSize;

    @ExceptionHandler(FileStorageException.class)
    public String handleFileStorageException(FileStorageException e, RedirectAttributes redirectAttributes,
                                             HttpServletRequest request) {
        redirectAttributes.addFlashAttribute(MESSAGE_ATTRIBUTE, e.getMessage());
        return HttpUtil.redirectToReferer(request);
    }

    @ExceptionHandler(FileListingException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleFileListingException() {
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
        redirectAttributes.addFlashAttribute(MESSAGE_ATTRIBUTE, e.getMessage());
        return HttpUtil.redirectToReferer(request);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e, RedirectAttributes redirectAttributes, HttpServletRequest request) {
        log.warn("MaxUploadSizeExceededException occurred: Uploaded file exceeds maximum size", e);
        redirectAttributes.addFlashAttribute(MESSAGE_ATTRIBUTE, "The uploaded file is too large. Maximum allowed size is " + maxFileSize);
        return HttpUtil.redirectToReferer(request);
    }
}
