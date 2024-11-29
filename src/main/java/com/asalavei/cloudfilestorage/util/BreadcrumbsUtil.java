package com.asalavei.cloudfilestorage.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

import static com.asalavei.cloudfilestorage.common.Constants.HOME_URL;
import static com.asalavei.cloudfilestorage.util.PathUtil.DELIMITER;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BreadcrumbsUtil {

    public static List<Breadcrumb> buildBreadcrumbs(String path) {
        List<Breadcrumb> breadcrumbs = new ArrayList<>();
        breadcrumbs.add(new Breadcrumb("Home", HOME_URL));

        if (!isFolder(path)) {
            path = path.substring(0, path.lastIndexOf(DELIMITER) + 1);
        }

        String[] parts = path.split(DELIMITER);
        StringBuilder currentPath = new StringBuilder(DELIMITER);

        for (String part : parts) {
            if (!part.isBlank()) {
                currentPath.append(part).append(DELIMITER);
                breadcrumbs.add(new Breadcrumb(part, currentPath.toString()));
            }
        }

        return breadcrumbs;
    }

    public record Breadcrumb(String name, String path) {
    }

    private static boolean isFolder(String path) {
        return path.endsWith(DELIMITER);
    }
}
