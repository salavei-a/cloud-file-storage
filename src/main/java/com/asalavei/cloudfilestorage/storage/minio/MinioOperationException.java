package com.asalavei.cloudfilestorage.storage.minio;

public class MinioOperationException extends RuntimeException {

    public MinioOperationException(String message) {
        super(message);
    }

    public MinioOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
