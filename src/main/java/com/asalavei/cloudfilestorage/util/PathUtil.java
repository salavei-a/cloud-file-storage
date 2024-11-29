package com.asalavei.cloudfilestorage.util;

import com.asalavei.cloudfilestorage.storage.ObjectResponseDto;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PathUtil {

    public static final String DELIMITER = "/";

    public static boolean isFolder(String path) {
        return path.endsWith(DELIMITER);
    }

    public static String getObjectName(String path) {
        if (isFolder(path)) {
            return getFolderName(path);
        } else {
            return getFileName(path);
        }
    }

    public static String getFolderName(String path) {
        return getFileName(path.substring(0, path.length() - 1));
    }

    public static String getFileName(String path) {
        return path.substring(path.lastIndexOf(DELIMITER) + 1);
    }

    public static String getRelativePath(String fullPath, String prefix) {
        return fullPath.replace(prefix, "");
    }

    public static String buildNewPath(String path, String newName) {
        String parentPath = getParentPath(path);
        return isFolder(path) ? parentPath + newName + DELIMITER : parentPath + newName;
    }

    public static String getParentPath(String path) {
        String pathToExtractParent = isFolder(path) ? path.substring(0, path.length() - 1) : path;
        return pathToExtractParent.substring(0, pathToExtractParent.lastIndexOf(DELIMITER) + 1);
    }

    public static String getParentFolderPath(String path) {
        return path.substring(0, path.lastIndexOf(DELIMITER) + 1);
    }

    public static List<ObjectResponseDto> getParentFolders(String path) {
        List<ObjectResponseDto> parentFolders = new ArrayList<>();
        StringBuilder currentPath = new StringBuilder(DELIMITER);

        for (String part : path.split(DELIMITER)) {
            if (!part.isEmpty() && path.contains(part + DELIMITER)) {
                currentPath.append(part).append(DELIMITER);
                parentFolders.add(
                        ObjectResponseDto.builder()
                                .name(part)
                                .path(currentPath.toString())
                                .isFolder(true)
                                .build()
                );
            }
        }

        return parentFolders;
    }

    public static String generateZipFilename(String path) {
        String timestamp = LocalDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'"));
        return getFolderName(path) + "-" + timestamp + ".zip";
    }
}
