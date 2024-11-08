package com.asalavei.cloudfilestorage.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BreadcrumbsUtil {

    public static List<Breadcrumb> buildBreadcrumbs(String path) {
        List<Breadcrumb> breadcrumbs = new ArrayList<>();
        breadcrumbs.add(new Breadcrumb("Home", "/"));

        String[] parts = path.split("/");
        StringBuilder currentPath = new StringBuilder();

        for (String part : parts) {
            if (!part.isBlank()) {
                currentPath.append(part).append("/");
                breadcrumbs.add(new Breadcrumb(part, currentPath.toString()));
            }
        }

        return breadcrumbs;
    }

    public record Breadcrumb(String name, String path) {
    }
}
