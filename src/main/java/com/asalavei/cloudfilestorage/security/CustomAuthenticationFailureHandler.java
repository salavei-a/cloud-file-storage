package com.asalavei.cloudfilestorage.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.support.SessionFlashMapManager;

import java.io.IOException;

import static com.asalavei.cloudfilestorage.common.Constants.ERROR_MESSAGE_ATTRIBUTE;
import static com.asalavei.cloudfilestorage.common.Constants.SIGNIN_URL;

@Slf4j
@Component
public class CustomAuthenticationFailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        FlashMap flashMap = new FlashMap();

        if (exception instanceof BadCredentialsException) {
            flashMap.put(ERROR_MESSAGE_ATTRIBUTE, "Incorrect username or password.");
        } else {
            log.error("An unexpected authentication error occurred", exception);
            flashMap.put(ERROR_MESSAGE_ATTRIBUTE, "An unexpected error occurred. Please try again later.");
        }

        SessionFlashMapManager flashMapManager = new SessionFlashMapManager();
        flashMapManager.saveOutputFlashMap(flashMap, request, response);

        response.sendRedirect(SIGNIN_URL);
    }
}
