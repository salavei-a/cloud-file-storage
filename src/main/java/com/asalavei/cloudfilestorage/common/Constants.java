package com.asalavei.cloudfilestorage.common;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Constants {

    public static final String USER_ATTRIBUTE = "user";
    public static final String ERROR_MESSAGE_ATTRIBUTE = "errorMessage";

    public static final String REDIRECT_HOME = "redirect:/";
    public static final String REDIRECT_SIGNIN = "redirect:/auth/signin";

    public static final String HOME_URL = "/";
    public static final String SIGNIN_URL = "/auth/signin";
    public static final String SIGNUP_URL = "/auth/signup";

    public static final String SIGNIN_VIEW = "auth/signin";
    public static final String SIGNUP_VIEW = "auth/signup";
    public static final String HOME_VIEW = "home";
    public static final String SEARCH_VIEW = "search";
    public static final String ERROR_404_VIEW = "error/404";
    public static final String ERROR_500_VIEW = "error/500";
}
