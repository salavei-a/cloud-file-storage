package com.asalavei.cloudfilestorage.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import static com.asalavei.cloudfilestorage.util.Constants.HOME_URL;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class HttpUtil {

    public static String redirectToReferer(HttpServletRequest request) {
        return "redirect:" + getReferer(request);
    }

    private static String getReferer(HttpServletRequest request) {
        String referer = request.getHeader("Referer");

        if (referer == null || referer.isBlank()) {
            referer = HOME_URL;
        }

        return referer;
    }
}
