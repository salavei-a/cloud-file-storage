package com.asalavei.cloudfilestorage.auth.user;

import com.asalavei.cloudfilestorage.exception.AppRuntimeException;

public class UserAlreadyExistsException extends AppRuntimeException {

    public UserAlreadyExistsException(String message) {
        super(message);
    }
}
