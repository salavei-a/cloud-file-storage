package com.asalavei.cloudfilestorage.storage.minio;

import com.asalavei.cloudfilestorage.exception.AppRuntimeException;

public class MinioOperationException extends AppRuntimeException {

    public MinioOperationException(String message) {
        super(message);
    }

    public MinioOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
