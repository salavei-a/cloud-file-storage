package com.asalavei.cloudfilestorage.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Constants {

    public static final String PATH_PARAM = "path";
    public static final String FILES_PARAM = "files";
    public static final String QUERY_PARAM = "query";

    public static final String USER_ATTRIBUTE = "user";
    public static final String OBJECTS_ATTRIBUTE = "objects";
    public static final String BREADCRUMBS_ATTRIBUTE = "breadcrumbs";
    public static final String MESSAGE_ATTRIBUTE = "message";
    public static final String ERROR_MESSAGE_ATTRIBUTE = "errorMessage";

    public static final String HOME_URL = "/";
    public static final String SIGNIN_URL = "/auth/signin";
    public static final String SIGNOUT_URL = "/auth/signout";
    public static final String PROCESS_SIGNIN_URL = "/auth/process-signin";

    public static final String REDIRECT_HOME = "redirect:" + HOME_URL;
    public static final String REDIRECT_SIGNIN = "redirect:" + SIGNIN_URL;
    public static final String FORWARD_PROCESS_SIGNIN = "forward:" + PROCESS_SIGNIN_URL;

    public static final String SIGNIN_VIEW = "auth/signin";
    public static final String SIGNUP_VIEW = "auth/signup";
    public static final String HOME_VIEW = "home";
    public static final String SEARCH_VIEW = "search";
    public static final String ERROR_404_VIEW = "error/404";
    public static final String ERROR_500_VIEW = "error/500";

    public static final String SESSION_COOKIE_NAME = "JSESSIONID";
    public static final String ANONYMOUS_USER = "anonymousUser";
}
