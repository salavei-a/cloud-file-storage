package com.asalavei.cloudfilestorage.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import static com.asalavei.cloudfilestorage.common.Constants.HOME_URL;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class HttpUtils {

    public static String redirectToReferer(HttpServletRequest request) {
        return "redirect:" + HttpUtils.getReferer(request);
    }

    public static String getReferer(HttpServletRequest request) {
        String referer = request.getHeader("Referer");

        if (referer == null || referer.isBlank()) {
            referer = HOME_URL;
        }

        return referer;
    }
}
