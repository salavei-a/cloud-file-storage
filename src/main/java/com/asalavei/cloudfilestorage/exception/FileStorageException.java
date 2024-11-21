package com.asalavei.cloudfilestorage.exception;

public class FileStorageException extends AppRuntimeException {

    public FileStorageException(String message) {
        super(message);
    }

    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}