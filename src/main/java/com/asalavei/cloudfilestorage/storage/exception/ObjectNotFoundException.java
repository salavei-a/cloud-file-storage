package com.asalavei.cloudfilestorage.storage.exception;

import com.asalavei.cloudfilestorage.exception.AppRuntimeException;

public class ObjectNotFoundException extends AppRuntimeException {

    public ObjectNotFoundException(String message) {
        super(message);
    }
}
